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
                        // 1. 生成环境变量文件
                        writeFile file: 'app_env.tmp', text: "${APP_ENV}"
                        
                        // 2. 生成部署脚本 (关键修改：把逻辑写入文件，而不是塞进 ssh 命令里)
                        // 注意：这里使用 Groovy 的多行字符串，Jenkins 会自动替换 ${DOCKER_USERNAME} 等变量
                        def deployScript = """#!/bin/bash
                        set -e
                        
                        # 准备配置目录
                        mkdir -p /opt/firmament/config
                        
                        # 移动环境变量文件
                        mv /tmp/application-prod.env.tmp /opt/firmament/config/application-prod.env
                        chmod 600 /opt/firmament/config/application-prod.env
                        
                        # 拉取镜像
                        echo "正在拉取镜像..."
                        docker pull ${DOCKER_USERNAME}/firmament-server:latest
                        
                        # 删除旧容器 (使用 || true 忽略报错，防止第一次部署时因为没有容器而停止)
                        echo "清理旧容器..."
                        docker stop firmament-server || true
                        docker rm firmament-server || true
                        
                        # 启动新容器
                        echo "启动新容器..."
                        docker run -d \\
                            --name firmament-server \\
                            --network firmament_app-network \\
                            --env-file /opt/firmament/config/application-prod.env \\
                            ${DOCKER_USERNAME}/firmament-server:latest
                        """
                        
                        // 写入部署脚本到本地
                        writeFile file: 'deploy.sh', text: deployScript
                        
                        // 3. 执行传输和运行
                        sh """
                            # --- 准备 SSH Key ---
                            mkdir -p ~/.ssh
                            cp "${SSH_KEY}" ~/.ssh/deploy_key
                            chmod 600 ~/.ssh/deploy_key
                            
                            # --- 上传文件 (环境变量 + 部署脚本) ---
                            echo "正在上传文件到远程服务器..."
                            # scp 支持一次传多个文件，或者分两次
                            scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no app_env.tmp ${SSH_USER}@${SERVER_HOST}:/tmp/application-prod.env.tmp
                            scp -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no deploy.sh ${SSH_USER}@${SERVER_HOST}:/tmp/deploy.sh
                            
                            # --- 远程执行脚本 ---
                            echo "正在执行远程部署..."
                            ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no ${SSH_USER}@${SERVER_HOST} "chmod +x /tmp/deploy.sh && bash /tmp/deploy.sh"
                            
                            # --- 清理本地临时文件 ---
                            rm -f ~/.ssh/deploy_key app_env.tmp deploy.sh
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