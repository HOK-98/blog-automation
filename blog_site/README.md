# AutoBlog dashboard

워드프레스 자동 생성 글을 MariaDB에 저장하고, JSP 화면에서 목록형 대시보드로 보여주는 기본 구현입니다.

## 1. MariaDB 준비

```sql
SOURCE database/recovery.sql;
```

## 2. DB 접속 정보

커밋에 인증정보가 포함되지 않도록 실제 설정 파일은 Git에서 제외되어 있습니다.

`src/main/resources/ApplicationContext.example.xml`을
`src/main/resources/ApplicationContext.xml`로 복사한 뒤 DB, GA4, Python 경로를 환경에 맞게 수정하세요.

MariaDB 애플리케이션 계정은 `database/create_blog_user.sql`의 비밀번호 자리표시자를 바꾼 뒤 생성합니다.

## 3. Python 자동화 준비

외부 Python 프로젝트가 `C:\ProjectAll\blog`에 있을 때 다음 명령으로 전용 환경을 준비합니다.

```powershell
python -m venv C:\ProjectAll\blog\.venv
C:\ProjectAll\blog\.venv\Scripts\python.exe -m pip install -r C:\ProjectAll\blog\requirements.txt
```

## 4. 필요한 Java 라이브러리

Tomcat에 배포하기 전에 아래 라이브러리를 프로젝트의 `WEB-INF/lib` 또는 서버 공용 lib 경로에 추가해야 합니다.

- `mariadb-java-client`
- `mybatis`
- `mybatis-spring`
- `spring-core`
- `spring-beans`
- `spring-context`
- `spring-web`
- `spring-webmvc`
- `commons-dbcp2`
- `commons-pool2`
- `commons-logging`

Java 1.8 기준으로는 보통 아래 계열이 안전합니다.

- Spring Framework `5.3.x`
- MyBatis `3.5.x`
- MyBatis-Spring `2.0.x`
- Tomcat `9.x`

현재 `WEB-INF/lib` 에 직접 넣어둔 jar:

- `spring-aop-5.3.39.jar`
- `spring-beans-5.3.39.jar`
- `spring-context-5.3.39.jar`
- `spring-core-5.3.39.jar`
- `spring-expression-5.3.39.jar`
- `spring-jcl-5.3.39.jar`
- `spring-jdbc-5.3.39.jar`
- `spring-tx-5.3.39.jar`
- `spring-web-5.3.39.jar`
- `spring-webmvc-5.3.39.jar`
- `mybatis-3.5.19.jar`
- `mybatis-spring-2.1.2.jar`
- `mariadb-java-client-3.3.3.jar`
- `commons-dbcp2-2.9.0.jar`
- `commons-pool2-2.11.1.jar`
- `commons-logging-1.2.jar`

## 5. 접속 경로

- `/`
- `/posts`

두 경로 모두 대시보드 화면으로 연결됩니다.

## 6. 구조

- Controller: `com.autoblog.controller`
- Service: `com.autoblog.service`
- DAO: `com.autoblog.dao`

## 7. MyBatis / Spring XML

- Spring Root Context: `src/main/resources/ApplicationContext.xml`
- Spring MVC Context: `src/main/webapp/WEB-INF/spring-servlet.xml`
- MyBatis 설정: `src/main/resources/mybatis-config.xml`
- SQL Mapper: `src/main/resources/mappers/BlogPostMapper.xml`
