pipeline {
    agent any
    
tools {
        maven 'Maven-3.9.11'
    }
    
    environment {
        // 从 Jenkins Credentials 中读取
        DOCKER_USERNAME = credentials('docker-username')
        SERVER_HOST = credentials('server-host')
        APPLICATION_PROD_ENV = credentials('application-prod-env')
    }
    
    stages {
        stage('拉取代码') {
            steps {
                checkout scm
            }
        }
        
        stage('Maven 打包') {
            steps {
                sh '''
                    mvn clean package -DskipTests
                '''
            }
        }
        
        stage('构建并推送 Docker 镜像') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh '''
                            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                            docker build -t ${DOCKER_USER}/firmament-server:latest -f firmament-server/Dockerfile ./firmament-server
                            docker push ${DOCKER_USER}/firmament-server:latest
                        '''
                    }
                }
            }
        }
        
        stage('部署到服务器') {
            steps {
                script {
                    withCredentials([
                        sshUserPrivateKey(
                            credentialsId: 'server-ssh-key',
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        ),
                        string(credentialsId: 'server-host', variable: 'SERVER_HOST'),
                        string(credentialsId: 'application-prod-env', variable: 'APP_ENV'),
                        string(credentialsId: 'docker-username', variable: 'DOCKER_USERNAME')
                    ]) {
                        // 先将环境变量内容写入临时文件
                        writeFile file: 'app_env.tmp', text: "${APP_ENV}"
                        
                        // 使用双引号以便 Jenkins 替换变量
                        sh """
                            # 创建临时 SSH 目录
                            mkdir -p ~/.ssh
                            cp \$SSH_KEY ~/.ssh/deploy_key
                            chmod 600 ~/.ssh/deploy_key
                            
                            # 使用 SSH 部署（Jenkins 会替换 ${DOCKER_USERNAME}，heredoc 使用单引号避免 shell 再次替换）
                            ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no \${SSH_USER}@\${SERVER_HOST} bash << 'ENDSSH'
                                # 1. 创建存放配置文件的目录
                                mkdir -p /opt/firmament/config
                                
                                # 2. 从标准输入读取环境变量文件内容
                                cat > /opt/firmament/config/application-prod.env
                                chmod 600 /opt/firmament/config/application-prod.env
                                
                                # 3. 拉取最新的镜像
                                docker pull ${DOCKER_USERNAME}/firmament-server:latest
                                
                                # 4. 停止并删除旧容器
                                docker stop firmament-server || true
                                docker rm firmament-server || true
                                
                                # 5. 启动新容器
                                docker run -d \\
                                    --name firmament-server \\
                                    --network firmament_app-network \\
                                    --env-file /opt/firmament/config/application-prod.env \\
                                    ${DOCKER_USERNAME}/firmament-server:latest
                            ENDSSH < app_env.tmp
                            
                            # 清理临时文件
                            rm -f ~/.ssh/deploy_key app_env.tmp
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            cleanWs() // 清理工作空间
        }
    }
}