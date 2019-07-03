# FlavorDemo
通过Gradle构建出不同风格的Apk
通常项目只会有一个主流版本，随着产品的发展可能会派生出不同的版本，比如，横竖屏界面，或者新产品的体验版，又或者针对不同硬件做适配等等，这些版本的代码与主流分支的代码并没有太大区别，仅仅是少部分的代码或界面有差异，同时又需要与现有版本一起并行开发，在未来分支版本也可能变成主流版本，在这种情况下可以使用代码管理工具，创建不同的代码分支进行管理。但随着时间推移，这种方式就会出现多个分支代码管理混乱。比如，公共部分存在的同一个 bug，就需要合并到各个分支，如果代码差异比较大，还需要在多个分支进行单独修复。

先不说是否容易改错改漏，平时的维护也会花费不少精力。为了保持优雅的工作方式，我们需要构建出不同版本的 APK ，同时只需维护一份代码，这样我们的日常工作就更简化了。

Gradle 本身就支持这种方式的配置，每个构建变体都代表不同应用版本。例如，我们希望构建App的免费版本（只提供有限的内容）和付费版本（提供更多内容）；还可以针对不同的设备、API 级别或其他设备变体构建App的不同版本。

构建变体是 Gradle 按照特定规则集合，并在构建类型和产品风格中配置的设置、代码和资源所生成的结果。我们并不会直接配置构建变体，而是配置组成变体的构建类型和产品风格。

## 配置buildTypes构建类型
可以在模块级 build.gradle 文件的 android 代码块内部创建和配置构建类型。
```java
//build.gradle 文件
 android {
     ...
     defaultConfig {
         applicationId "com.flavordemo"
         ...
     }
     buildTypes {//Android Studio 会默认自动创建debug和release这两种构建类型
         release {
             minifyEnabled false
             manifestPlaceholders = [app_name: "app", app_icon: "@mipmap/ic_launcher"]
             proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
         }

         debug {
             applicationIdSuffix ".debug"
             manifestPlaceholders = [app_name: "app_debug", app_icon: "@mipmap/ic_launcher"]
             debuggable true
         }

         preview {//此处定义了一个构建类型preview，
             initWith debug //该构建类型从debug构建类型复制属性
             applicationIdSuffix ".preview"
             manifestPlaceholders = [app_name: "app_preview", app_icon: "@mipmap/ic_launcher"] //可以利用该标签生成一些环境变量，如：meta-data
         }
     }
     ...
 }

 //AndroidManifest.xml
 <application
     ...
     android:icon="${app_icon}" //通过${}访问在manifestPlaceholders声明的值
     android:label="${app_name}"
     ...>
```

> PS：一般情况下，我们都不需要在 buildTypes 里增加自己的构建类型，默认的 debug 以及 release 已经足够，但是可以针对 debug 和 release 配置不同的属性，比如：release 版本需要混淆，签名文件不同，开启资源压缩等等。

## 配置productFlavors产品风格
创建产品风格与创建构建类型相似：只需将其添加到构建配置中的 productFlavors 代码块并加入所需的设置即可。产品风格支持与 defaultConfig 相同的属性，这是因为 defaultConfig 实际上属于 ProductFlavor 类。
```java
//build.gradle 文件
android {
   ...
   flavorDimensions "channle", "version" //属于较高优先级风格维度的产品风格首先显示，之后是较低优先级维度的产品风格，再之后是构建类型。
   productFlavors {
       dev { //所有风格必须属于指定的风格维度
           dimension "channle"
           applicationIdSuffix ".dev" //com.flavordemo.dev.debug
           versionNameSuffix "-dev" //1.0-dev
       }

       stable {
           dimension "channle"
           applicationIdSuffix ".stable"
           versionNameSuffix "-stable"
       }

       v1 { //所有风格命名必须是字符串类型
           dimension "version"
           versionName "V1." + android.defaultConfig.versionName //V1.1.0-dev
           versionCode 10000 + android.defaultConfig.versionCode //10001
       }

       v2 {
           dimension "version"
           versionName "V2." + android.defaultConfig.versionName
           versionCode 20000 + android.defaultConfig.versionCode
       }
   }
   ...
}


//BuildConfig.java
public final class BuildConfig {
   public static final boolean DEBUG = Boolean.parseBoolean("true");
   public static final String APPLICATION_ID = "com.flavordemo.dev.debug"; //根据产品风格的优先度生成的包名
   public static final String BUILD_TYPE = "debug";
   public static final String FLAVOR = "devV1";
   public static final int VERSION_CODE = 10001;
   public static final String VERSION_NAME = "V1.1.0-dev"; //默认的版本号应该是1.0-dev，但是由于version纬度修改了版本号的命名规则，最后采用该纬度的规则
   public static final String FLAVOR_channle = "dev";
   public static final String FLAVOR_version = "v1";
 }
```

以上面的构建配置为例，Gradle 可以使用以下命名方案创建总共 12 个构建变体：

    构建变体：[dev, stable][V1, V2][Debug, Release, Preview]
    对应 APK：app-[dev, stable]-[v1, v2]-[debug, release, preview].apk
    例如，
    构建变体：devV1Debug
    对应 APK：app-dev-v1-debug.apk

> PS：大多数时候，我们都是在该代码块中配置自己的产品风格，首先要做的是确定产品的纬度应该怎么划分，常见的划分纬度是按机型划分，按 api 级别划分，以及应用版本划分。所有 defaultConfig 能支持的配置，在这里都可以使用。配置的优先顺序是按照 flavorDimensions 从左往右的次序，最后才是 buildTypes。

## 过滤变体
Gradle 会配置的产品风格与构建类型的每个可能的组合创建构建变体。不过，某些特定的构建变体在项目环境中并不必要，也可能没有意义。可以在模块级 build.gradle 文件中创建一个变体过滤器，以移除某些构建变体配置。
```java
//build.gradle 文件
android {
     ...
     variantFilter { variant ->
         def names = variant.getName() //获取完整的build variant名称，如：devV1Debug
         if ((names.contains("dev") && names.contains("Release")) //没有dev分支且release的版本，也没有stable分支且debug的版本
                 || (names.contains("stable") && names.contains("Debug"))) {
             setIgnore(true) //设置忽略
         }
     }
     ...
 }
```
这样我们就过滤了 dev 分支的 release 版本以及 stable 分支的 debug 版本，以上例子设置忽略后的结果：
![过滤后的结果](https://mmbiz.qpic.cn/mmbiz_png/KZYic25zrWRveqSibyaEWATpn9zTKHZNyqicdVUS1LzEQpcH37hkY2xmCQxm3AoajiaR3BwZmFTJMhd4ZAljSwpUicw/0?wx_fmt=png)
> PS：过滤变体的作用是为了减少一部分不需要的产品风格，减少管理的复杂度，这类产物一般是存在问题的或者业务代码根本是走不通的。比如这里的例子就是 dev 版本不可能存在 release 的这种组合，为了避免错误的发生，就不应该生成这类风格的 apk。

## 创建source sets源集

除了可以为各个产品风格和构建变体创建源集目录外，也可以为每个产品风格组合创建源集目录。例如，可以创建 Java 源并将其添加到 src/stableV2/java/ 目录中，Gradle 仅会在构建组合了这两种产品风格的变体时使用这些源。与属于各个产品风格的源集相比，为产品风格组合创建的源集拥有更高的优先级。

默认情况下，Android Studio 会创建 main/ 源集和目录，用于存储所有构建变体之间共享的一切资源。然而，也可以创建新的源集来控制 Gradle 要为特定的构建类型、产品风格（以及使用风格维度时的产品风格组合）和构建变体编译和打包的确切文件。例如，可以在 main/ 源集中定义基本的功能，使用产品风格源集针对不同的客户更改应用的品牌，或者仅针对使用调试构建类型的构建变体包含特殊的权限和日志记录功能。

当创建新的构建变体时，Android Studio 不会为我们创建源集目录，但会提供几个选项，帮助创建目录。例如，要为“debug”构建类型只创建 java/ 目录，请执行以下操作：
1. 打开 Project 窗格，并从窗格顶部的下拉菜单中选择 Project 视图。
2. 导航至 MyProject/app/src/。
3. 右键点击 src 目录并选择 New > Folder > Java Folder。
4. 从 Target Source Set 旁边的下拉菜单中，选择 debug。
5. 点击 Finish。

Android Studio 将会为debug构建类型创建源集目录，然后在该目录内部创建 java/ 目录。或者，在针对特定的构建变体向我们的项目中添加新文件时，也可以让 Android Studio 为我们创建目录。例如，要为“debug”构建类型创建 XML 值文件：
1. 在相同的 Project 窗格中，右键点击 src 目录并选择 New > XML > Values XML File。
2. 为 XML 文件输入名称或保留默认名称。
3. 从 Target Source Set 旁边的下拉菜单中，选择 debug。
4. 点击 Finish。

由于“debug”构建类型被指定为目标源集，Android Studio 会在创建 XML 文件时自动创建必要的目录。最终的目录结构看上去应该类似于下图：

![创建源集](https://mmbiz.qpic.cn/mmbiz_png/KZYic25zrWRveqSibyaEWATpn9zTKHZNyq45dMQcVZRQNoT3eapoCxDsXESudleVsGyVu4zQyHC2PlibxSj1icgxTw/0?wx_fmt=png)

按照同样的方法，还可以为产品风格创建源集目录（例如 src/stable/），为构建变体创建源集目录（例如 src/stableV2/）。此外，还可以创建针对特定构建变体的测试源集，例如 src/androidTestStableDebug/。下图为Android Studio提示的源集目录：

![源集目录](https://mmbiz.qpic.cn/mmbiz_png/KZYic25zrWRveqSibyaEWATpn9zTKHZNyq3T9Z5fonu0Yc4eYTlDJWQbIud3zKCmNmOlpJj9Z4S8AXEEuRLpkw0w/0?wx_fmt=png)
> PS：使用 AndroidStudio 提供的创建向导，我们可以方便的为产品建立不同的资源目录。建立资源目录主要的作用是区分不同产品风格差异化的实现。配置合并的规则是：productFlavors > buildTypes > defaultConfig，意思是 productFlavors 里重复定义的会覆盖buildTypes里重复定义的，buildTypes里定义的会覆盖defaultConfig。另外 productFlavors 内部的覆盖规则是根据 flavorDimensions 定义的倒序覆盖。

## 更改默认源配置
如果源未组织到 Gradle 期望的默认源集文件结构中（如上面的创建源集部分中所述），可以使用 sourceSets 代码块更改 Gradle 希望为源集的每个组件收集文件的位置。
```java
//build.gradle 文件
android {
     ...
     sourceSets {
         v1 { //把v1构建变体的所有资源指向other目录
             //默认路径是'src/main/java'
             java.srcDirs = ['other/java']

             //默认路径是'src/main/res'
             res.srcDirs = ['other/res1', 'other/res2/layouts', 'other/res2/strings'] //指定多个资源目录，比如res1资源，res2存放layout和values

             //默认路径是'src/main/'
             manifest.srcFile 'other/AndroidManifest.xml'
         }

         debug { //指定debug使用特定的so文件目录
             jniLibs.srcDirs = ['src/debug/jniLibs_debug']
         }
     }
     ...
 }
```
我们为 v1 构建变体的代码以及资源都指向了 other 目录，并且指定了 debug 变体 so 文件的目录位置。最终的目录结构看上去应该类似于下图：
![修改scr文件](https://mmbiz.qpic.cn/mmbiz_png/KZYic25zrWRveqSibyaEWATpn9zTKHZNyqujFqGNia7fdHPxZQueib8zBniaMWJ2HxraU7FFBJr1rgjFrVu3VDJuUyA/0?wx_fmt=png)
> PS：区分源目录的作用主要是部分产品风格会有一些特殊的实现，这些差异化的实现需要在最终打包出来的 apk 有所体现。举个例子，假设目前最新 v2 版本，但是 v1 到 v2 做了一个比较大的重构，所有底层依赖以及部分接口实现都发生了不兼容的修改，此时，对于 v2 版本来说，重构的目的是为了抛弃历史遗留代码带来的约束。但最要命的是目前 v1 版本与 v2 版本依然在并行的维护，两个版本既有一共有的依赖也有差异的实现，关键是将来还可能会完全剥离。此时就可以为 v1 版本指定一个独立的源文件目录，为个别文件指定特殊的路径。主流实现保留在 main 文件里。

## 指定变体构建依赖
Gradle 会为我们定义好的产品风格生成对应的 implementation DLS 函数，我们可以按不同的产品风格引用不同的外部依赖。直接在 dependencies 代码块使用即可，该代码块与 android 同一级别。
```java
//build.gradle 文件
//默认情况下只会生成产品风格单一纬度的DSL函数，如果需要复合纬度的函数，需要先声明后使用，否则会提示错误：Could not find method
devV1Implementation()
 configurations {
     devV1Implementation
 }

 dependencies {
     implementation fileTree(dir: 'libs', include: ['*.jar'])

     //单一产品风格纬度
     v1Implementation files('libs_v1/v1.jar') //构建所有带v1的变体时都会引用，如：devV1Debug、devV1Preview、stableV1Preview、stableV1Release
     v2Implementation files('libs/lib.jar')

     devImplementation files('lib_dev/dev.jar') //构建所有带dev的变体时都会引用，如：devV1Debug、devV1Preview、devV2Debug、devV2Preview
     stableImplementation files('libs/lib.jar')

     //复合产品风格纬度
     devV1Implementation files('lib_dev_v1/dev_v1.jar')  //构建所有带dev、v1的变体时都会引用，如：devV1Debug、devV1Preview

     //该方式也支持远程依赖
 }
```
> PS：前面的所有设置都是针对内部代码结构的，但是除了内部结构的划分以外，对于外部依赖有时也需要针对不同产品风格进行选择性的依赖，比如 v1 到 v2 做了大量的重构，底层实现发生了改变，需要根据 v1 引用相应的 api ，根据 v2 又引用另外的 api 依赖，此时完全可以增加一层中间层 adapter 的接口抽象层，去引用有区别的底层实现，并把 adapter 层分别放到各自的源目录下，这样上层实现就只需要关注各自的 adapter 接口层即可。

## 生成变体各自的BuildConfig
Gradle 也可以设置一些看起来像系统属性或环境变量的项目属性，以便根据不同的构建变体定制不同的环境变量。直接在 gradle.properties 文件定义即可。
```java
//gradle.properties文件
 //Setting a project property via a system property
 com.flavordemo.project.key.v1=v1_value
 com.flavordemo.project.key.v2=v2_value
 //Setting a project property via an environment variable
 COM_FALVORDEMO_PROJECT_key_dev=dev_value
 COM_FALVORDEMO_PROJECT_key_stable=stable_value


 //build.gradle 文件
 android {
     ...
     applicationVariants.all { variant ->
         variant.outputs.all { output ->
             def names = variant.getName()
             //channel变体特有属性
             if (names.contains("dev")) {
                 buildConfigField "String", "KEY_CHANNLEL", "\"${COM_FALVORDEMO_PROJECT_key_dev}\""
             } else if (names.contains("stable")) {
                 buildConfigField "String", "KEY_CHANNLEL", "\"${COM_FALVORDEMO_PROJECT_key_stable}\""
             }
             //公共属性
             buildConfigField "String", "KEY_COMMON", "\"${COM_FALVORDEMO_PROJECT_key_common}\""
         }
     }
     ...
 }
```
> PS：除了依赖关系以外，很多应用以及一些第三方sdk都具有各自的运行环境配置，常用的做法是把一部分值声明在 gradle.properties 文件里，然后在代码里使用 BuildConfig 类进行引用，如果代码层已经做了区分，那么相应的配置文件也需要修改时，就可以使用这个方法为每个不同的构建变体声明各自的值。另外，还可以通过 manifestPlaceholders 来为 AndroidManifest.xml 做一些差异化的配置，详细可以看回 buildTypes 的配置那里。

## 最后
以上就是 Gradle 提供的所有相关配置，通过设置，项目的所有代码可以保留在一个工程内部，结合不同的配置以及资源文件最终可以高效管理工程，代码及资源管理只需要维护不同产品风格差异化的那一部分。由于 AndroidStudio 原生支持这种管理方式，大大减少了维护的人力及时间投入，无论产品线怎么变化都可以方便的扩展。

如有问题可以关注我的公众号留言：  
![公众号](https://mmbiz.qpic.cn/mmbiz_jpg/KZYic25zrWRviaLBMia8djBc9S0Uon3nfQE2uyMv6O2lyC8L80wpRyflFtg190UAxBTZQxhnZsQ6gNkMhRts1bpHg/0?wx_fmt=jpeg)

---
<font color=Gray size=2>参考：
https://developer.android.com/studio/build/build-variants</font>
