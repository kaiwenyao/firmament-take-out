# 基础镜像
FROM eclipse-temurin:17-jre-alpine

# 设置时区为上海
ENV TZ=Europe/Dublin
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 工作目录
WORKDIR /app

# 接收构建参数（Jar包的名字）
ARG JAR_FILE=target/*.jar

# 将 jar 包复制进去并重命名为 app.jar
COPY ${JAR_FILE} app.jar

# 暴露端口
EXPOSE 8080

# 启动命令：生产环境 profile 通过参数覆盖
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
