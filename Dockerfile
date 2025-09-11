# Используйте официальный образ с Java 21
FROM eclipse-temurin:21-jdk-alpine

# Установите рабочую директорию
WORKDIR /app

# Копируем JAR файл
COPY target/*.jar app.jar

# Открываем порт
EXPOSE 8081

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]