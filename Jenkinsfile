// =============================================================================
// firmament-take-out 持续集成流水线
// =============================================================================
// 运行形态：每次构建由 Jenkins Kubernetes 插件在集群里临时创建一个 Pod 作为
// 构建代理，构建结束后 Pod 销毁。Pod 里有两个业务容器，分别承担不同阶段：
//
//   maven  —— 编译、单元测试、集成测试、打包、部署（内含 JDK 17 与 Maven）
//   docker —— 构建并推送镜像（内含 docker CLI）
//
// 插件会为 jnlp 补齐默认镜像和连接参数；这里显式声明它的资源预算。
// 流水线的每个 steps 默认落在 jnlp 容器里，所以凡是要用 maven 或 docker 的步骤，
// 都必须用 container('maven') / container('docker') 显式切换。
//
// 阶段顺序：拉代码 → 静态分析（Checkstyle + SpotBugs） → 单元测试 → 集成测试
//           → 打包 → 构建推送镜像 → 部署（仅 main 分支）
// =============================================================================
pipeline {
    agent {
        kubernetes {
            // Jenkins「系统管理 → 节点和云」中配置的 Kubernetes 云名称。
            // 若那里改了名字，这里要同步，否则构建会因找不到云而排队不动。
            cloud 'kubernetes'

            // 直接在流水线里内联 Pod 定义，而不是引用 Jenkins UI 上预设的 Pod Template。
            // 好处是构建环境随代码一起版本化：改动可评审、可回滚，也不依赖某台
            // Jenkins 实例的界面配置。
            yaml '''
apiVersion: v1
kind: Pod
metadata:
  labels:
    ci.kaiwen.dev/workload: maven-build
spec:
  # 三个 Java 项目及其分支共用标签：同一节点最多一个 Maven 构建 Pod。
  # 节点忙时排队，不将构建挤到同一台机器；无需硬编码节点名。
  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchLabels:
              ci.kaiwen.dev/workload: maven-build
          topologyKey: kubernetes.io/hostname
  containers:
    # -------------------------------------------------------
    # 容器一：maven —— 编译、测试、打包、部署
    # -------------------------------------------------------
    # command/args 覆写成 sleep 是 Jenkins K8s 插件的固定写法：容器必须保持存活，
    # 等流水线用 container('maven') 进来执行命令。若不覆写，maven 镜像跑完默认
    # 入口就退出了，Pod 随即失败。
    - name: maven
      image: maven:3.9.12-eclipse-temurin-17
      command:
        - sleep
      args:
        - "9999999"
      tty: true
      # 与 jnlp 容器共享的工作区挂载点，代码检出后各容器都能看到同一份文件
      workingDir: /home/jenkins/agent
      # 与 eventpulse-backend 的已用预算一致；限制覆盖 Maven 及其测试 JVM。
      resources:
        requests:
          cpu: "500m"
          memory: 2Gi
        limits:
          cpu: "2000m"
          memory: 4Gi
      # -----------------------------------------------------
      # Testcontainers 在本环境下的两处必要调整
      # -----------------------------------------------------
      # 背景：集成测试用 Testcontainers 启动真实的 MySQL 和 Redis。它是一个 Java 库，
      # 通过 Docker API 拉起容器、把容器端口随机映射到宿主机，再把映射后的地址回填给
      # 测试代码。这里的关键事实是——容器并不运行在这个 maven 容器「内部」，而是通过
      # 挂进来的 socket 交给宿主节点的 Docker 守护进程创建，是这个 Pod 的「兄弟」容器。
      env:
        # 【调整一】告诉 Testcontainers 该用哪个地址连它起的容器。
        #
        # Testcontainers 默认假定「谁调用 Docker，容器就映射到谁的 localhost」，于是
        # 返回 localhost:<映射端口>。但这里守护进程在宿主节点上，端口映射到的是节点网卡；
        # 而 Pod 有独立的网络命名空间，Pod 里的 localhost 是 Pod 自己，不是节点。
        # 结果就是连不上，典型报错 "Could not connect to Ryuk at localhost:<port>"。
        #
        # 用 Downward API（K8s 把 Pod 自身的元数据注入为环境变量的机制）取到所在节点的
        # IP，再让 Testcontainers 改用该 IP 回连。
        - name: TESTCONTAINERS_HOST_OVERRIDE
          valueFrom:
            fieldRef:
              fieldPath: status.hostIP
        # 【调整二】关闭 Ryuk。
        #
        # Ryuk 是 Testcontainers 默认附带的「看门狗」容器：测试进程一断开连接，它就
        # 负责删掉本次创建的所有容器，防止残留。但它需要 --privileged 才能运行，
        # 受限集群通常拉不起来，反而阻塞整个测试。
        #
        # 关掉后回退到 JVM shutdown hook 清理：测试进程正常结束时自行删除容器。
        # 代价是进程被强杀（构建取消、OOM、超时）时 hook 不执行，容器会残留在节点上，
        # 因此流水线末尾的 post { always } 另做了一道按标签的兜底清理。
        - name: TESTCONTAINERS_RYUK_DISABLED
          value: "true"
      volumeMounts:
        # Maven 本地仓库（依赖缓存），挂节点本地 hostPath，与 eventpulse-backend
        # 共用同一份缓存，详见下方 volumes
        # 挂 /var/cache/maven/repository 而非镜像默认的 /root/.m2/repository：
        # 每个 mvn 显式传 -Dmaven.repo.local 指向这里，不依赖镜像的默认用户/
        # 家目录，将来换镜像（如非 root 变体）也不会悄悄和缓存失联
        - mountPath: /var/cache/maven/repository
          name: maven-repo
        # 宿主节点的 Docker 守护进程 socket。集成测试阶段 Testcontainers 需要它
        # 才能创建 MySQL / Redis 容器。
        #
        # ⚠️ 这是一处需要留意的信任边界。能写这个 socket，就能对宿主节点的 Docker
        # 下达任意指令——包括挂载节点根目录再起一个特权容器。换句话说，凡是能让代码
        # 在测试阶段执行的人，实际上都拿到了节点的 root 权限。
        #
        # 当前仓库私有、提交者可信，接受这一风险以换取真实依赖的集成测试。若将来对外
        # 开放贡献（任何人提 PR 即触发构建），必须先换成隔离方案，例如：
        #   - DinD sidecar：Pod 内单起一个 Docker 守护进程，DOCKER_HOST 指向它，
        #     不挂宿主 socket，爆炸半径限于 Pod；
        #   - Testcontainers Cloud：容器托管在外部，构建节点完全不暴露 Docker。
        - mountPath: /var/run/docker.sock
          name: docker-sock

    # -------------------------------------------------------
    # 容器二：docker —— 构建并推送镜像，以及清理测试残留容器
    # -------------------------------------------------------
    # 只装了 docker CLI，没有 Docker 守护进程；实际工作交给下面挂进来的宿主机
    # socket 上的守护进程执行。
    - name: docker
      image: docker:latest
      command:
        - sleep
      args:
        - "9999999"
      tty: true
      workingDir: /home/jenkins/agent
      # 只限制 CLI；经宿主 socket 创建的容器/镜像构建不受此上限约束。
      resources:
        requests:
          cpu: "100m"
          memory: 128Mi
        limits:
          cpu: "500m"
          memory: 256Mi
      volumeMounts:
        # docker CLI 通过这个 socket 指挥宿主节点的 Docker 守护进程干活
        - mountPath: /var/run/docker.sock
          name: docker-sock

    # 保留插件默认镜像和连接参数，仅补充 Agent 的 request / limit。
    - name: jnlp
      resources:
        requests:
          cpu: "100m"
          memory: 256Mi
        limits:
          cpu: "500m"
          memory: 512Mi

  # -------------------------------------------------------
  # 卷定义
  # -------------------------------------------------------
  volumes:
    # Maven 本地仓库：节点本地 hostPath，与 eventpulse-backend 等所有 Java 流水线
    # 共用同一路径。相比原先的 NFS PVC（jenkins-maven-cache）：依赖解析和 JAR
    # 读取不再走 NFS，构建更快；代价是缓存按节点各存一份，新节点首次构建要
    # 全量下载一次（DirectoryOrCreate 自动建目录）。
    # 同一节点上并发构建共享此仓库且会并发写，因此每个 mvn 必须带
    # file-lock + file-gav 跨进程锁参数——与 eventpulse 的约定一致，缺了就可能
    # 互相踩坏元数据。
    - name: maven-repo
      hostPath:
        path: /var/cache/jenkins/maven/repository
        type: DirectoryOrCreate

    # 宿主节点的 Docker 守护进程 socket，供上面两个容器共用
    - name: docker-sock
      hostPath:
        path: /var/run/docker.sock
'''
        }
    }

    environment {
        // 本次构建的唯一标识，用作 Testcontainers 容器的 Docker 标签。
        // 集成测试给它起的每个容器都打上这个标签，末尾 post { always } 就能只删
        // 属于本次构建的容器，不会误伤同一节点上并发构建正在使用的容器。
        //
        // BUILD_TAG 是 Jenkins 内置变量，形如 jenkins-<任务名>-<构建号>；多分支
        // 流水线的任务名含 '/' 等字符，这里统一替换成 '-' 以免影响标签匹配。
        IT_BUILD_TAG = "${env.BUILD_TAG}".replaceAll('[^A-Za-z0-9_.-]', '-')
    }

    stages {
        stage('1. 拉取代码') {
            steps {
                checkout scm
            }
        }

        stage('2. 静态代码分析') {
            steps {
                container('maven') {
                    // SpotBugs（字节码缺陷） + Checkstyle（源码约定）静态分析门禁。
                    //
                    // 为什么放在测试之前：静态分析不依赖数据库/Redis，只需编译产物，
                    // 一两分钟内就能给出反馈；风格或低级缺陷没必要等到跑完测试才暴露。
                    //
                    // 命令拆解：
                    //   - checkstyle:check 只读源码，不需编译；显式列出保证阶段自含。
                    //   - compile 产出 SpotBugs 所需的 class（validate 阶段还会自动跑一遍
                    //     绑定的 checkstyle:check，与上面的显式调用规则完全一致）。
                    //   - spotbugs:check 分析编译产物，超过阈值（Medium）即失败。
                    // 配置文件（checkstyle.xml / spotbugs-exclude.xml）在仓库根目录，
                    // 两个插件都按工作目录的相对路径解析，必须在仓库根目录执行 mvn——
                    // 与 Jenkins 检出布局一致。
                    //
                    // 报告归档：checkstyle-result.xml 与 spotbugsXml.xml 供下载复查；
                    // 后续阶段的 clean 会清理 target，所以必须在本阶段内立即归档。
                    sh '''
                        echo "运行静态代码分析（Checkstyle + SpotBugs）"
                        mvn checkstyle:check compile spotbugs:check \\
                            -Dmaven.repo.local=/var/cache/maven/repository \\
                            -Daether.syncContext.named.factory=file-lock \\
                            -Daether.syncContext.named.nameMapper=file-gav
                    '''
                    archiveArtifacts artifacts: '**/target/checkstyle-result.xml, **/target/spotbugsXml.xml', allowEmptyArchive: true
                }
            }
        }

        stage('3. 单元测试') {
            steps {
                container('maven') {
                    // 单元测试都是切片测试（@WebMvcTest 只加载 Web 层、其余依赖用
                    // Mockito 打桩），不连数据库、Redis 或任何外部服务，因此跑得快
                    // 且无需额外准备环境。也不激活 prod profile，避免把生产密钥带进测试。
                    //
                    // -Dtest 的 '!' 前缀是排除语法：跑除 dev.kaiwen.it 包以外的全部测试，
                    // 集成测试留给下一阶段。failIfNoSpecifiedTests=false 让筛选后没有
                    // 匹配用例的模块直接跳过，而不是判定构建失败。
                    sh '''
                        echo "运行单元测试（切片测试，无需外部服务）"
                        mvn -pl firmament-server -am clean test \\
                            -Dtest='!dev.kaiwen.it.**' \\
                            -Dsurefire.failIfNoSpecifiedTests=false \\
                            -Djacoco.exec.file=jacoco-ut.exec \\
                            -Dmaven.repo.local=/var/cache/maven/repository \\
                            -Daether.syncContext.named.factory=file-lock \\
                            -Daether.syncContext.named.nameMapper=file-gav
                    '''
                }
            }
        }

        stage('4. 集成测试') {
            steps {
                // 集成测试启动完整 Spring 上下文，配 Testcontainers 拉起真实的 MySQL 和
                // Redis 来跑 REST 接口，因此覆盖到 SQL 方言、事务、缓存等 Mock 测不到的行为。
                //
                // 这些数据库容器由宿主节点的 Docker 守护进程创建（经上面挂载的 socket），
                // 是本 Pod 的兄弟容器而非嵌套在内；每次构建全新创建、测完销毁，互不干扰。
                container('maven') {
                    sh '''
                        echo "运行 REST API 集成测试（Testcontainers: MySQL + Redis）"
                        # IntegrationTestBase 读取该变量，给它创建的容器打上本次构建的标签，
                        # 供末尾 post 阶段做兜底清理
                        export FIRMAMENT_IT_BUILD_TAG="$IT_BUILD_TAG"
                        mvn -pl firmament-server -am test \\
                            -Dspring.profiles.active=it \\
                            -Dtest='dev.kaiwen.it.**' \\
                            -Dsurefire.failIfNoSpecifiedTests=false \\
                            -Djacoco.exec.file=jacoco-it.exec \\
                            -Dmaven.repo.local=/var/cache/maven/repository \\
                            -Daether.syncContext.named.factory=file-lock \\
                            -Daether.syncContext.named.nameMapper=file-gav
                        echo "合并单元测试与集成测试的 JaCoCo exec 并生成报告"
                        mvn -pl firmament-common,firmament-server \\
                            jacoco:merge@merge-coverage jacoco:report@report-merged \\
                            -Dmaven.repo.local=/var/cache/maven/repository \\
                            -Daether.syncContext.named.factory=file-lock \\
                            -Daether.syncContext.named.nameMapper=file-gav
                    '''
                    archiveArtifacts artifacts: '**/target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('5. Maven 打包') {
            steps {
                container('maven') {
                    // 跳过测试：前两个阶段已经跑过全部单元与集成测试，
                    // 这里只要产出 Jar，重复跑一遍纯属浪费构建时间。
                    echo '构建 Jar 包...'
                    sh '''
                        mvn clean package -DskipTests \\
                            -Dmaven.repo.local=/var/cache/maven/repository \\
                            -Daether.syncContext.named.factory=file-lock \\
                            -Daether.syncContext.named.nameMapper=file-gav
                    '''
                }
            }
        }

        stage('6. 构建并推送 Docker 镜像') {
            // changeRequest() 在构建来自 Pull Request 时为真。PR 只需验证代码能过测试，
            // 不该往镜像仓库推产物，因此这一步跳过。
            when {
                not { changeRequest() }
            }
            steps {
                container('docker') {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                            // 工作区目录的属主与本容器内的当前用户不一致时，Git 会以
                            // "dubious ownership" 为由拒绝操作。把目录标记为可信来放行，
                            // 好让下面能读到 commit 号用作镜像标签。
                            sh '''
                                git config --global --add safe.directory ${WORKSPACE} || true
                                git config --global --add safe.directory "$(pwd)" || true
                            '''

                            def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                            def branchName = env.BRANCH_NAME ?: sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()

                            echo "当前分支: ${branchName}, Commit Hash: ${gitCommit}"

                            // 登录镜像仓库并构建。docker 命令实际由宿主节点的守护进程
                            // 执行（经挂载的 socket），构建产物落在节点的镜像库里。
                            // --password-stdin 避免密码出现在进程命令行中。
                            sh '''
                                echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                                docker build -t $DOCKER_USER/firmament-server:latest -f firmament-server/Dockerfile ./firmament-server
                            '''

                            // 标签策略：Git tag → 用 tag 名；main 分支 → commit 号 + 构建号 + latest；
                            // 其他分支 → dev-<分支名>-<commit>，便于区分来源且不覆盖主线镜像。
                            if (env.TAG_NAME) {
                                sh '''
                                    docker tag $DOCKER_USER/firmament-server:latest $DOCKER_USER/firmament-server:''' + env.TAG_NAME + '''
                                    docker push $DOCKER_USER/firmament-server:''' + env.TAG_NAME + '''
                                    docker push $DOCKER_USER/firmament-server:latest
                                '''
                            } else if (branchName == 'main' || branchName == 'master') {
                                sh '''
                                    docker tag $DOCKER_USER/firmament-server:latest $DOCKER_USER/firmament-server:commit-''' + gitCommit + '''
                                    docker push $DOCKER_USER/firmament-server:commit-''' + gitCommit + '''
                                    docker tag $DOCKER_USER/firmament-server:latest $DOCKER_USER/firmament-server:build-''' + env.BUILD_NUMBER + '''
                                    docker push $DOCKER_USER/firmament-server:build-''' + env.BUILD_NUMBER + '''
                                    docker push $DOCKER_USER/firmament-server:latest
                                '''
                            } else {
                                def safeBranchName = branchName.replace("/", "-").replace("_", "-")
                                sh '''
                                    docker tag $DOCKER_USER/firmament-server:latest $DOCKER_USER/firmament-server:dev-''' + safeBranchName + '-' + gitCommit + '''
                                    docker push $DOCKER_USER/firmament-server:dev-''' + safeBranchName + '-' + gitCommit + '''
                                '''
                            }
                        }
                    }
                }
            }
        }

        stage('7. 部署到服务器') {
            // 只有合入 main 后的构建才部署；PR 构建即便目标分支是 main 也不部署。
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                container('maven') {
                    script {
                        withCredentials([
                            sshUserPrivateKey(credentialsId: 'server-ssh-key', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER'),
                            string(credentialsId: 'server-host', variable: 'SERVER_HOST'),
                            string(credentialsId: 'docker-username', variable: 'DOCKER_USERNAME'),
                            file(credentialsId: 'application-prod-env', variable: 'APP_ENV_FILE')
                        ]) {
                            // 1. 把凭据库里的生产环境变量文件复制到工作区，准备上传。
                            //    file 类型凭据由 Jenkins 落成临时文件，路径经 APP_ENV_FILE 给出。
                            sh "cp ${APP_ENV_FILE} app_env.tmp"

                            // 2. 在 Jenkins 端拼出部署脚本，再整份送到服务器执行。
                            //    这里用 Groovy 的双引号三引号字符串，其中的 ${...} 在拼装时就被替换成真实值，
                            //    所以送到服务器的是一份不含变量的成品脚本，无需再费心传参和转义。
                            //    （若改用单引号，Groovy 不做插值，变量会原样留到远端而取不到值。）
                            def deployScript = """#!/bin/bash
                                set -e
                                mkdir -p /opt/firmament/config
                                mv /tmp/application-prod.env.tmp /opt/firmament/config/application-prod.env
                                chmod 600 /opt/firmament/config/application-prod.env

                                # 此时脚本里的变量已经是真实值了，例如 docker pull kaiwen/firmament...
                                echo "正在拉取镜像: ${DOCKER_USERNAME}/firmament-server:latest"
                                docker pull ${DOCKER_USERNAME}/firmament-server:latest

                                docker stop firmament-server || true
                                docker rm firmament-server || true

                                docker run -d \\
                                    --name firmament-server \\
                                    --restart=unless-stopped \\
                                    --network firmament_app-network \\
                                    --env-file /opt/firmament/config/application-prod.env \\
                                    -p 127.0.0.1:8080:8080\\
                                    ${DOCKER_USERNAME}/firmament-server:latest
                            """

                            // 3. 落成文件以便 scp 上传
                            writeFile file: 'deploy.sh', text: deployScript

                            // 4. 上传环境变量文件与部署脚本，远端执行后清理本地痕迹。
                            //    StrictHostKeyChecking=no 用于跳过首次连接的指纹确认，
                            //    否则非交互式 SSH 会在此挂起。
                            sh """
                                mkdir -p ~/.ssh
                                cat "${SSH_KEY}" > ~/.ssh/deploy_key
                                chmod 600 ~/.ssh/deploy_key

                                # 上传
                                scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no app_env.tmp ${SSH_USER}@${SERVER_HOST}:/tmp/application-prod.env.tmp
                                scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no deploy.sh ${SSH_USER}@${SERVER_HOST}:/tmp/deploy.sh

                                # 执行：脚本内的变量已在 Jenkins 端替换完毕，直接跑即可
                                echo "正在远程执行部署脚本..."
                                ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no ${SSH_USER}@${SERVER_HOST} "bash /tmp/deploy.sh"

                                # 清理私钥与含密文件，避免留在工作区被后续步骤读到
                                rm -f ~/.ssh/deploy_key app_env.tmp deploy.sh
                            """
                        }
                    }
                }
            }
        }
    }

    // post 块无论构建成功、失败还是被取消都会执行，用来做收尾清理。
    post {
        always {
            // 兜底删除集成测试残留的容器。
            //
            // 正常路径下 Testcontainers 会在测试进程退出时自行清理（Ryuk 已禁用，见
            // 上方说明）；但构建被取消或进程被强杀时来不及清理，容器就滞留在节点上
            // 长期占用内存和端口。这里按本次构建的专属标签精确删除，因此不会影响
            // 同一节点上其他正在进行的构建。
            script {
                try {
                    container('docker') {
                        sh '''
                            echo "清理本次构建残留的 Testcontainers 容器（标签: $IT_BUILD_TAG）"
                            stray=$(docker ps -aq --filter "label=dev.kaiwen.it.build=$IT_BUILD_TAG" || true)
                            if [ -n "$stray" ]; then
                                docker rm -f $stray || true
                            else
                                echo "无残留容器"
                            fi
                        '''
                    }
                } catch (err) {
                    // 清理属于尽力而为：Pod 没起来或 docker 容器不可用时，
                    // 不应因此把本来成功的构建判为失败。
                    echo "Testcontainers 残留清理跳过: ${err}"
                }
            }
            // 清空工作区，释放节点磁盘
            cleanWs()
        }
    }
}
