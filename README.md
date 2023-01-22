<img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/logo/logo-simple.png" alt="apollo-logo" width="40%">

# apollo-git-publish
一个用于将维护在git项目中的apollo配置文件一件发布到apollo配置中心的工具。

（用于apollo配置在开发阶段用git项目管理，发版本时需要全量配置更新到apollo中验证。由于apollo的api只提供单个配置的更新删除功能，故采用多线程，检查一个namesapce下面的所有已经发布的配置，对比本地文件中配置的配置，计算需要删除、新增及更新的配置，最小化调用api去更新并发布apollo中的配置）

## 快速使用方式
1. 使用main分支，将项目配置文件直接写到项目中，目录格式参考项目中SampleApp文件夹。
文件夹命名规则如下：
```
${appid}
｜-- ${env}
    ｜-- ${cluster}
        ｜-- ${namespace}.properties
```
2. 修改resources/apollo.properties中的apollo配置信息，**token在apollo的dashboard上申请，注意要逐个项目对token授权app权限**
```
apollo.url=http://localhost:8070/
apollo.token=1b8ac319ce8f64b07adb0fed89f2c5edf18447c0
apollo.releaseBy=apollo
apollo.releaseComment=default comment by apollo-git-publish
apollo.releaseTitle=default title by apollo-git-publish
```
3. 直接运行PublishToApollo.java中的Main方法即可，按提示操作即可。

## native应用（graal vm）
可以借助graal vm将项目编译成可执行文件，直接在命令行允许即可。需要自己安装graal vm，借助maven的native-image-maven-plugin插件打包可执行文件。

使用native应用，需要配置文件.apollo-git-publish-cnf到用户home目录(~/.apollo-git-publish-cnf)，配置文件参考如下：
```
apollo.url=http://localhost:8070/
apollo.token=0e613b93607739c2733eb1b20dff93611f701b8f
apollo.releaseBy=apollo
apollo.releaseComment=default comment by apollo-git-publish
apollo.releaseTitle=default title by apollo-git-publish

file.url=~/zerotobeone/apollo-git-publish
```
其中file.url配置你的apollo配置文件项目的根目录即可。
