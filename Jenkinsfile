pipeline {
    // 1. 使用你刚才配置的 Pod Template 标签 
    agent {
        label 'firmament-build'
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
        
        stage('3. Maven 打包') {
            steps {
                // 进入 maven 容器执行 
                container('maven') {
                    echo '构建 Jar 包...'
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('4. 构建并推送 Docker 镜像') {
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
        
        stage('5. 部署到服务器') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                // 部署逻辑通常涉及 SSH/SCP，建议在 maven 容器（带完整的 OS 工具）中执行 
                container('maven') {
                    script {
                        withCredentials([
                            sshUserPrivateKey(
                                credentialsId: 'server-ssh-key',
                                keyFileVariable: 'SSH_KEY',
                                usernameVariable: 'SSH_USER'
                            ),
                            string(credentialsId: 'server-host', variable: 'SERVER_HOST'),
                            string(credentialsId: 'docker-username', variable: 'DOCKER_USERNAME'),
                            file(credentialsId: 'application-prod-env', variable: 'APP_ENV_FILE')
                        ]) {
                            sh '''
                                cp "$APP_ENV_FILE" app_env.tmp
                            '''
                            
                            // 使用单引号避免敏感信息插值，通过环境变量传递
                            // 在脚本生成时通过 shell 命令替换环境变量，避免 Groovy 插值
                            sh '''
                                cat > deploy.sh << 'DEPLOY_SCRIPT_EOF'
                                #!/bin/bash
                                set -e
                                mkdir -p /opt/firmament/config
                                mv /tmp/application-prod.env.tmp /opt/firmament/config/application-prod.env
                                chmod 600 /opt/firmament/config/application-prod.env
                                docker pull ${DOCKER_USERNAME}/firmament-server:latest
                                docker stop firmament-server || true
                                docker rm firmament-server || true
                                docker run -d \\
                                    --name firmament-server \\
                                    --network firmament_app-network \\
                                    --env-file /opt/firmament/config/application-prod.env \\
                                    ${DOCKER_USERNAME}/firmament-server:latest
                                DEPLOY_SCRIPT_EOF
                            '''
                            
                            sh '''
                                mkdir -p ~/.ssh
                                cp "$SSH_KEY" ~/.ssh/deploy_key
                                chmod 600 ~/.ssh/deploy_key
                                scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no app_env.tmp "$SSH_USER@$SERVER_HOST:/tmp/application-prod.env.tmp"
                                scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no deploy.sh "$SSH_USER@$SERVER_HOST:/tmp/deploy.sh"
                                # 在远程执行时，使用 env 命令设置环境变量并执行脚本
                                # 使用 '"$VAR"' 组合：'退出单引号，"进入双引号让shell展开$VAR，'重新进入单引号
                                ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no "$SSH_USER@$SERVER_HOST" \
                                    'env DOCKER_USERNAME='"'"'"$DOCKER_USERNAME"'"'"' bash /tmp/deploy.sh'
                                rm -f ~/.ssh/deploy_key app_env.tmp deploy.sh
                            '''
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