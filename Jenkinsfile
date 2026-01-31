pipeline {
    // 1. 使用你刚才配置的 Pod Template 标签
    // agent {
    //     label 'firmament-build'
    // }

    // 换成直接配置pod template的模式
    agent {
        kubernetes {
            // 指向你在 Jenkins 系统管理里配置的云名称，通常默认为 "kubernetes"
            cloud 'kubernetes'

            // 下面这段 YAML 完全复刻了你截图中的 Pod Template 配置
            yaml '''
apiVersion: v1
kind: Pod
metadata:
  labels:
    # 对应截图中的 "标签列表: firmament-build"
    jenkins/label: firmament-build
spec:
  containers:
    # -------------------------------------------------------
    # 1. Maven 容器配置 (对应截图 image_230741)
    # -------------------------------------------------------
    - name: maven
      image: maven:3.9.12-eclipse-temurin-17
      command:
        - sleep
      args:
        - "9999999"
      tty: true
      workingDir: /home/jenkins/agent
      volumeMounts:
        # 挂载 Maven 缓存
        - mountPath: /root/.m2/repository
          name: jenkins-maven-cache
          readOnly: false

    # -------------------------------------------------------
    # 2. Docker 容器配置 (对应截图 image_230746)
    # -------------------------------------------------------
    - name: docker
      image: docker:latest
      command:
        - sleep
      args:
        - "9999999"
      tty: true
      workingDir: /home/jenkins/agent
      volumeMounts:
        # 挂载宿主机 Docker Socket
        - mountPath: /var/run/docker.sock
          name: docker-sock

  # -------------------------------------------------------
  # 3. 卷定义 (对应截图 image_230762)
  # -------------------------------------------------------
  volumes:
    # PVC: 对应 "Persistent Volume Claim: jenkins-maven-cache"
    - name: jenkins-maven-cache
      emptyDir: {}

    # HostPath: 对应 "Host Path Volume: /var/run/docker.sock"
    - name: docker-sock
      hostPath:
        path: /var/run/docker.sock
'''
        }
    }
    // 2. 移除 tools 部分，因为我们现在直接使用容器里的 Maven

    environment {
        DOCKER_USERNAME = credentials('docker-username')
        SERVER_HOST = credentials('server-host')
        APPLICATION_PROD_ENV = credentials('application-prod-env')
    }

    stages {
        stage('1. 拉取代码') {
            steps {
                checkout scm
            }
        }

        stage('2. 单元测试') {
            steps {
                // 进入 maven 容器执行
                container('maven') {
                    script {
                        withCredentials([
                            file(credentialsId: 'application-prod-env', variable: 'APP_ENV_FILE')
                        ]) {
                            sh '''
                                cp ${APP_ENV_FILE} application-prod.env
                                echo "已加载生产环境配置文件"
                                set -a
                                . ./application-prod.env
                                set +a
                                mvn -Dspring.profiles.active=prod test
                            '''
                        }
                    }
                }
            }
        }

        stage('3. SonarQube 代码质量分析') {
            steps {
                // 进入 maven 容器执行
                container('maven') {
                    // 'sonar-server' 必须和你 Jenkins 系统配置里的 Name 一致
                    withSonarQubeEnv('sonar-server') {
                        // 这里不需要传 -Dsonar.login，插件会自动处理认证
                        sh 'mvn clean verify sonar:sonar'
                    }
                }
            }
        }

        stage('4. Maven 打包') {
            steps {
                // 进入 maven 容器执行
                container('maven') {
                    echo '构建 Jar 包...'
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('5. 构建并推送 Docker 镜像') {
            when {
                not { changeRequest() }
            }
            steps {
                // 进入 docker 容器执行
                container('docker') {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                            // 配置 Git 安全目录，解决容器中用户权限问题
                            sh '''
                                git config --global --add safe.directory ${WORKSPACE} || true
                                git config --global --add safe.directory "$(pwd)" || true
                            '''

                            def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                            def branchName = env.BRANCH_NAME ?: sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()

                            echo "当前分支: ${branchName}, Commit Hash: ${gitCommit}"

                            // 登录并构建 (已通过 .sock 挂载使用宿主机 Docker)
                            sh '''
                                echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                                docker build -t $DOCKER_USER/firmament-server:latest -f firmament-server/Dockerfile ./firmament-server
                            '''

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

        stage('6. 部署到服务器') {
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
                            // 1. 准备环境变量文件
                            sh "cp ${APP_ENV_FILE} app_env.tmp"

                            // 2. 使用 Groovy 生成脚本 (关键修改)
                            // 这里的 """ 三引号允许 Groovy 在 Jenkins 端直接把 ${DOCKER_USERNAME} 换成真实值
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
                                    --network firmament_app-network \\
                                    --env-file /opt/firmament/config/application-prod.env \\
                                    ${DOCKER_USERNAME}/firmament-server:latest
                            """

                            // 3. 将生成的脚本写入文件
                            writeFile file: 'deploy.sh', text: deployScript

                            // 4. 上传并执行 (SSH 命令大大简化)
                            sh """
                                mkdir -p ~/.ssh
                                cat "${SSH_KEY}" > ~/.ssh/deploy_key
                                chmod 600 ~/.ssh/deploy_key

                                # 上传
                                scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no app_env.tmp ${SSH_USER}@${SERVER_HOST}:/tmp/application-prod.env.tmp
                                scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no deploy.sh ${SSH_USER}@${SERVER_HOST}:/tmp/deploy.sh

                                # 执行 (不再需要 env DOCKER_USERNAME=... 这种复杂的传参)
                                echo "正在远程执行部署脚本..."
                                ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no ${SSH_USER}@${SERVER_HOST} "bash /tmp/deploy.sh"

                                # 清理
                                rm -f ~/.ssh/deploy_key app_env.tmp deploy.sh
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}