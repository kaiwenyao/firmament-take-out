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
                        // 1. 将环境变量内容写入 Jenkins 工作空间的临时文件
                        writeFile file: 'app_env.tmp', text: "${APP_ENV}"
                        
                        sh """
                            # --- 准备 SSH Key ---
                            mkdir -p ~/.ssh
                            cp "${SSH_KEY}" ~/.ssh/deploy_key
                            chmod 600 ~/.ssh/deploy_key
                            
                            # --- 第一步：上传配置文件 (使用 SCP) ---
                            # 将本地的 app_env.tmp 上传到远程服务器的 /tmp 目录
                            echo "正在上传配置文件..."
                            scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no app_env.tmp ${SSH_USER}@${SERVER_HOST}:/tmp/application-prod.env.tmp
                            
                            # --- 第二步：远程执行部署命令 (使用 SSH) ---
                            echo "正在连接远程服务器执行部署..."
                            ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no ${SSH_USER}@${SERVER_HOST} << EOF
                                set -e  # 如果任何命令失败，立即退出
                                
                                # 1. 准备配置目录
                                mkdir -p /opt/firmament/config
                                
                                # 2. 将上传的临时文件移动到正式位置
                                mv /tmp/application-prod.env.tmp /opt/firmament/config/application-prod.env
                                chmod 600 /opt/firmament/config/application-prod.env
                                
                                # 3. 拉取最新的镜像 (Jenkins 变量替换)
                                echo "拉取镜像: ${DOCKER_USERNAME}/firmament-server:latest"
                                docker pull ${DOCKER_USERNAME}/firmament-server:latest
                                
                                # 4. 停止并删除旧容器
                                echo "停止旧容器..."
                                docker stop firmament-server || true
                                docker rm firmament-server || true
                                
                                # 5. 启动新容器
                                echo "启动新容器..."
                                docker run -d \\
                                    --name firmament-server \\
                                    --network firmament_app-network \\
                                    --env-file /opt/firmament/config/application-prod.env \\
                                    ${DOCKER_USERNAME}/firmament-server:latest
                            EOF
                            
                            # --- 清理本地临时文件 ---
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