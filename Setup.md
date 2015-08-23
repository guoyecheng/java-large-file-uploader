### java ###

This project is really easy to configure as the dependencies are a war containing a simple javascript file and a jar which includes all the web configuration (using web fragment from servlet 3.0 specification).
[Some properties](ConfigurableProperties.md) are exposed as MBeans and can be changed at runtime and others can be set directly in the javascript.

#### Maven ####

Just define the maven dependencies:
```
<dependency>
	<groupId>com.am</groupId>
	<artifactId>java-large-file-uploader-war</artifactId>
	<version>1.1.8</version>
	<type>war</type>
</dependency>
<dependency>
	<groupId>com.am</groupId>
	<artifactId>java-large-file-uploader-jar</artifactId>
	<version>1.1.8</version>
</dependency>
```
And the repository:
```
<repository>
	<id>java large file uploader repository</id>
	<url>http://java-large-file-uploader.googlecode.com/svn/mvnrepo</url>
</repository>
```

#### Spring ####

If you are not defining a `contextConfigLocation`, no action is required.
But if you are using Spring and you are defining your own `contextConfigLocation` in your web.xml, it will override the one defined in the web fragment.

Please add '`classpath*:/META-INF/jlfu-web-fragment-context.xml`' inside the param value.

Example:
```
<context-param>
	<param-name>contextConfigLocation</param-name>
	<param-value>
		classpath*:/META-INF/jlfu-web-fragment-context.xml
		/WEB-INF/spring/another-spring-configuration-file.xml
	</param-value>
</context-param>
```

**/!\** And do not forget to specify `version="3.0"` in the `web-app` element of your web.xml !

### javascript ###

See [JavaLargeFileUploader](JavaLargeFileUploader.md)