# 微服务治理框架(Java版)开发环境搭建与配置

## 安装gradle构建工具
东方证券gRPC服务治理框架的源代码使用 gradle 作为构建工具，gradle 要求版本为**4.9**或以上。配置环境变量GRADLE_HOME，并将 gradle 可执行文件追加到环境变量 PATH 中。

## 设置本地Maven缓存的存储目录
在 USER_HOME/.m2 目录下增加 settings.xml 文件，修改其中 localRepository 的值，指定本地Maven缓存的存储目录。

settings.xml文件可以从maven的安装包下的conf目录拷贝。


## 将生成的jar包和依赖添加到本地Maven缓存
切换到源代码根目录，执行以下命令：

	gradle clean install


## 配置依赖
以maven工程为例，增加如下依赖至pom.xml文件:

	<properties>
		<orientsec.grpc.version>1.2.4</orientsec.grpc.version>
	</properties>

	<!-- orientsec-grpc-java -->
	<dependency>
		<groupId>com.orientsec.grpc</groupId>
		<artifactId>orientsec-grpc-netty-shaded</artifactId>
		<version>${orientsec.grpc.version}</version>
	</dependency>
	<dependency>
		<groupId>com.orientsec.grpc</groupId>
		<artifactId>orientsec-grpc-protobuf</artifactId>
		<version>${orientsec.grpc.version}</version>
	</dependency>
	<dependency>
		<groupId>com.orientsec.grpc</groupId>
		<artifactId>orientsec-grpc-stub</artifactId>
		<version>${orientsec.grpc.version}</version>
	</dependency>

## 安装Zookeeper
安装Zookeeper，建议使用3.4.13或以上版本。