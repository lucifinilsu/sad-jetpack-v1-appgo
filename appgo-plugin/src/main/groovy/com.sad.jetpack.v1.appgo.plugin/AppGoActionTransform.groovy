package com.sad.jetpack.v1.appgo.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.sad.jetpack.v1.appgo.annotation.ApplicationAccess
import com.sad.jetpack.v1.appgo.annotation.ApplicationLifeCycleAction
import com.sad.jetpack.v1.utils.ClassScanner
import javassist.*
import org.gradle.api.Project

class AppGoActionTransform extends Transform implements ClassScanner.OnFileScannedCallback, ClassScanner.ITarget{
    private Project project
    private final static String BASIC_PACKAGE="com.sad.jetpack.v1.appgo";

    AppGoActionTransform(Project project){
        this.project=project
    }
    @Override
    String getName() {
        return "AppGoActionTransform"
    }

    //需要处理的数据类型，有两种枚举类型
    //CLASSES和RESOURCES，CLASSES代表处理的java的class文件，RESOURCES代表要处理java的资源
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return Collections.singleton(QualifiedContent.DefaultContentType.CLASSES)
    }

    //    EXTERNAL_LIBRARIES        只有外部库
    //    PROJECT                       只有项目内容
    //    PROJECT_LOCAL_DEPS            只有项目的本地依赖(本地jar)
    //    PROVIDED_ONLY                 只提供本地或远程依赖项
    //    SUB_PROJECTS              只有子项目。
    //    SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
    //    TESTED_CODE                   由当前变量(包括依赖项)测试的代码
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        /*Sets.immutableEnumSet(
                QualifiedContent.Scope.PROJECT,
                QualifiedContent.Scope.SUB_PROJECTS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES
                //QualifiedContent.Scope.PROVIDED_ONLY
        )*/
        if (project.plugins.hasPlugin("com.android.application")) {
            return Set.<QualifiedContent.Scope>of(QualifiedContent.Scope.PROJECT,
                    QualifiedContent.Scope.SUB_PROJECTS,
                    QualifiedContent.Scope.EXTERNAL_LIBRARIES)
            /*return Set.immutableEnumSet(
                    QualifiedContent.Scope.PROJECT,
                    QualifiedContent.Scope.SUB_PROJECTS,
                    QualifiedContent.Scope.EXTERNAL_LIBRARIES)*/
        } else if (project.plugins.hasPlugin("com.android.library") ||project.plugins.hasPlugin("java-library")) {
            return Set.of(QualifiedContent.Scope.PROJECT)
            /*return Sets.immutableEnumSet(
                    QualifiedContent.Scope.PROJECT)*/
        } else {
            return Collections.emptySet()
        }
    }

    //指明当前Transform是否支持增量编译
    @Override
    boolean isIncremental() {
        return false
    }
    /*private CtClass applicationParentClass
    private CtClass lifecyclesObserverInterface*/
    private ClassScanResult scanResult=new ClassScanResult()
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        ClassPool classPool = new ClassPool(true);//ClassPool.getDefault()
        //project.logger.error("==>project.android.bootClasspath="+project.android.bootClasspath)
        classPool.appendClassPath(project.android.bootClasspath[0].toString())
        //ClassScanner.scan(project,classPool,transformInvocation,this);
        ClassScanner.newInstance(project)
            .classPool(classPool)
            .transformInvocation(transformInvocation)
            .scannedCallback(this)
            .into(this)
    }

    private boolean avaliableClass(CtClass ctClass){
        return !Modifier.isAbstract(ctClass.getModifiers()) && !Modifier.isInterface(ctClass.getModifiers())
    }

    /*private Object getAnnotation(CtMethod method,Class<?> clz) throws ClassNotFoundException {
        MethodInfo mi = method.getMethodInfo2();
        AnnotationsAttribute ainfo = (AnnotationsAttribute)
        mi.getAttribute(AnnotationsAttribute.invisibleTag);
        AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
        mi.getAttribute(AnnotationsAttribute.visibleTag);
        return CtClassType.getAnnotationType(clz,
                method.getDeclaringClass().getClassPool(),
                ainfo, ainfo2);
    }*/

    @Override
    boolean onScanned(ClassPool classPool, File scannedFile, File dest) {

        CtClass applicationParentClass = classPool.get("android.app.Application")
        CtClass lifecyclesObserverInterface = classPool.get(BASIC_PACKAGE+".api.IApplicationLifecyclesObserver")
//        CtClass onCreatedPreInterface = classPool.get("com.sad.jetpack.architecture.appgo.api.IApplicationOnCreatePre")
//        CtClass contentProviderClass = classPool.get("com.sad.jetpack.architecture.appgo.api.ApplicationContextInitializerProvider")
        classPool.importPackage("android.os")
        classPool.importPackage("android.util")
        classPool.importPackage("android.content.res.Configuration")
        classPool.importPackage("android.util.Log")
        if (!scannedFile.name.endsWith("class")) {
            return false
        }
        //获取扫描到的class
        CtClass scannedClass=null
        InputStream is=null
        try {
            is=new FileInputStream(scannedFile)
            scannedClass = classPool.makeClass(is)
            is.close()
        } catch (Throwable throwable) {
            throwable.printStackTrace()
            project.logger.error("Parsing class file ${scannedFile.getAbsolutePath()} fail.", throwable)
            is.close()
            return false
        }
        if (!avaliableClass(scannedClass)){
            return false
        }
        boolean handled = false
        if (applicationParentClass != null && scannedClass.subclassOf(applicationParentClass)){
            Object accessAnnotation=scannedClass.getAnnotation(ApplicationAccess.class);
            if(accessAnnotation!=null){
                //Application的子类被注解过了，说明是可埋点的宿主，记录并等待处理
                project.logger.error(">> Host Application is "+scannedClass.name)
                scanResult.setApplicationClass(scannedClass);
                scanResult.setDest(dest)
                scanResult.setHandled(true)
                handled=false;
            }
        }
        //如果扫描到的类是Application行为监听实现类
        if (lifecyclesObserverInterface != null && scannedClass.getInterfaces().contains(lifecyclesObserverInterface)){
            boolean isHandleThisObserverClass=false
            project.logger.error(">>> find observer interface:"+scannedClass.name)
            //剔除没有任何显式注解埋点方法的的监听
            CtMethod[] methods = scannedClass.getDeclaredMethods()//.getMethods()
            for (CtMethod method:methods){
                Object annotation = method.getAnnotation(ApplicationLifeCycleAction.class)
                project.logger.error("^   find anchord method:"+method.name+" from "+scannedClass.name+" that has anntation ["+method.getAvailableAnnotations()+"] and info ["+method.getMethodInfo()+"]")
                if (annotation!=null){
                    //project.logger.error("^  find anchord method:"+method.name+" from "+scannedClass.name)
                    isHandleThisObserverClass=true
                    break
                }
            }
            if (isHandleThisObserverClass){
                scanResult.getLifecyclesObserverClassList().add(scannedClass)
            }
        }
        return handled








        /*
        //如果扫描到的class是Application的子类，则准备处理
        if (applicationParentClass != null && scannedClass.subclassOf(applicationParentClass)){
            Object accessAnnotation=scannedClass.getAnnotation(ApplicationAccess.class);
            if(accessAnnotation!=null){
                //Application的子类被注解过了，说明是可埋点的宿主，记录并等待处理
                project.logger.error(">> Host Application is "+scannedClass.name)
                scanResult.setApplicationClass(scannedClass);
                scanResult.setDest(dest)
                scanResult.setHandled(false)
                handled=false;
            }
        }

        //如果扫描到的类是Application行为监听实现类
        if (lifecyclesObserverInterface != null && scannedClass.getInterfaces().contains(lifecyclesObserverInterface)){
            boolean isHandleThisObserverClass=false
            project.logger.error(">>> find observer interface:"+scannedClass.name)
            //String rM=scannedClass.name.replace(".","/")
            //project.logger.error(">>> find observer interface rm:"+rM)
            //classPool.insertClassPath(rM)
            //剔除没有任何显式注解埋点方法的的监听
            CtMethod[] methods = scannedClass.getMethods()
            for (CtMethod method:methods){
                Object annotation = method.getAnnotation(ApplicationLifeCycleAction.class)
                project.logger.error("^   find anchord method:"+method.name+" from "+scannedClass.name+" that has anntation ["+method.getAvailableAnnotations()+"] and info ["+method.getMethodInfo()+"]")
                if (annotation!=null){
                    //project.logger.error("^  find anchord method:"+method.name+" from "+scannedClass.name)
                    isHandleThisObserverClass=true
                    break
                }
            }
            if (isHandleThisObserverClass){
                scanResult.getLifecyclesObserverClassList().add(scannedClass)
            }
        }
        return handled*/
    }

    @Override
    void onScannedCompleted(ClassPool classPool) {
        if (scanResult.getHandled()){
            setAnchor(scanResult,
                    "onCreate",
                    "onApplicationPreCreated",
                    "(Landroid/app/Application;)V",
                    null)
            setAnchor(scanResult,
                    "onCreate",
                    "onApplicationCreated",
                    "(Landroid/app/Application;)V",
                    null)
            setAnchor(scanResult,
                    "onConfigurationChanged",
                    "onApplicationPreConfigurationChanged",
                    "(Landroid/app/Application;Landroid/content/res/Configuration;)V",
                    classPool.get("android.content.res.Configuration")
            )
            setAnchor(scanResult,
                    "onConfigurationChanged",
                    "onApplicationConfigurationChanged",
                    "(Landroid/app/Application;Landroid/content/res/Configuration;)V",
                    classPool.get("android.content.res.Configuration")
            )

            setAnchor(scanResult,
                    "onLowMemory",
                    "onApplicationPreLowMemory",
                    "(Landroid/app/Application;)V",
                    null
            )
            setAnchor(scanResult,
                    "onLowMemory",
                    "onApplicationLowMemory",
                    "(Landroid/app/Application;)V",
                    null
            )

            setAnchor(scanResult,
                    "onTerminate",
                    "onApplicationPreTerminate",
                    "(Landroid/app/Application;)V",
                    null
            )
            setAnchor(scanResult,
                    "onTerminate",
                    "onApplicationTerminate",
                    "(Landroid/app/Application;)V",
                    null
            )
            setAnchor(scanResult,
                    "onTrimMemory",
                    "onApplicationPreTrimMemory",
                    "(Landroid/app/Application;I)V",
                    classPool.get(int.class.name)
            )
            setAnchor(scanResult,
                    "onTrimMemory",
                    "onApplicationTrimMemory",
                    "(Landroid/app/Application;I)V",
                    classPool.get(int.class.name)
            )
            setAnchor(scanResult,
                    "attachBaseContext",
                    "attachApplicationPreBaseContext",
                    "(Landroid/app/Application;Landroid/content/Context;)V",
                    classPool.get("android.content.Context")
            )
            setAnchor(scanResult,
                    "attachBaseContext",
                    "attachApplicationBaseContext",
                    "(Landroid/app/Application;Landroid/content/Context;)V",
                    classPool.get("android.content.Context")
            )
            //将埋点后的流写入class文件
            scanResult.getApplicationClass().writeFile(scanResult.getDest().absolutePath)
            scanResult.getApplicationClass().detach()

        }
    }

    private CtField createApplicationLifeCycleObserverField(CtClass observerClass,String fieldName,CtClass applicationClass){
        CtField field = null
        try {
            field=applicationClass.getDeclaredField(fieldName)
        }catch(Exception e){

        }

        if (field == null) {
            field = new CtField(observerClass, fieldName,applicationClass)
            field.setModifiers(Modifier.PRIVATE)
            applicationClass.addField(field)
        }
        return field
    }


    private void setAnchor(ClassScanResult scanResult,
            String overwriteApplicationMethodName,
            String anchorMethodName,
            String anchorMethodNameDesc,
            CtClass... anchorMethodParametersClass

    ){
        //String methodName="onConfigurationChanged"
        //CtClass anchorMethodParametersClass=classPool.get("android.content.res.Configuration")

        ArrayList<CtClass> org=scanResult.getLifecyclesObserverClassList();
        //String anchorMethodName=after?"onApplicationConfigurationChanged":"onApplicationPreConfigurationChanged"
        ArrayList<CtClass> applicationLifeCycleObserverList = Anchor.sort(org,anchorMethodName,anchorMethodNameDesc)
        String anchorCode="";
        StringBuilder ps = new StringBuilder()
        for (CtClass observerClass:applicationLifeCycleObserverList){
            CtMethod anchorMethod=null
            try {
                /*CtMethod[] ms=observerClass.getMethods()
                for (CtMethod m:ms){
                    project.logger.error(">> anchor "+observerClass.name+"'s method contains:info=["+m.methodInfo.toString()+"]")
                }*/
                anchorMethod=observerClass.getMethod(anchorMethodName,anchorMethodNameDesc)//getDeclaredMethod(anchorMethodName,classPool.get("android.app.Application"))

            }catch(Exception e){
                e.printStackTrace()
            }
            if (anchorMethod==null){
                project.logger.error(">> anchor "+observerClass.name+"'s method "+anchorMethodName+" is not found")
                continue
            }
            String fieldName="m"+observerClass.getSimpleName()
            createApplicationLifeCycleObserverField(observerClass,fieldName,scanResult.applicationClass)
            ApplicationLifeCycleAction action=anchorMethod.getAnnotation(ApplicationLifeCycleAction.class)
            if (action!=null){
                ps.append("try{\n")
                ps.append("if("+fieldName+"==null){"+fieldName+"="+"new "+observerClass.name+"()"+";}\n")
                String[] processes=action.processName()
                boolean hasIncludeProcess=(processes!=null && (processes.length>0));
                project.logger.error(">> include processNames? "+hasIncludeProcess+" : "+processes)
                String p="";
                if (anchorMethodParametersClass!=null && anchorMethodParametersClass.length>0){
                    p=",\$\$"
                }
                if (hasIncludeProcess){
                    String pn="pNames"+(applicationLifeCycleObserverList.indexOf(observerClass))
                    ps.append("java.lang.String[] "+pn+"=new java.lang.String["+processes.length+"];\n")
                    for (int i = 0; i < processes.length; i++) {
                        ps.append(pn+"["+i+"]="+"\""+processes[i]+"\";\n")
                    }
                    ps.append("if (java.util.Arrays.asList("+pn+").contains("+BASIC_PACKAGE+".api.ApplicationLifecycleObserverMaster.getCurrAppProcessName(this)))\n")
                    ps.append("{"+fieldName+"."+anchorMethodName+"(this"+p+");}")
                }
                else {
                    ps.append(fieldName+"."+anchorMethodName+"(this"+p+");")
                }
                ps.append("\n}\ncatch(java.lang.Exception e){\ne.printStackTrace();\n}")
            }
        }
        anchorCode=ps.toString()
        if (anchorCode!=null && !"".equals(anchorCode.toString())){
            CtMethod method=Anchor.getOverwirteMethodFromApplication(project,scanResult.applicationClass,overwriteApplicationMethodName,anchorMethodParametersClass)
            setAnchor(scanResult,method,anchorCode,!anchorMethodName.contains("Pre"))
        }
    }

    private void setAnchor(
            ClassScanResult scanResult,
            CtMethod method,
            String anchorCode,
            boolean afterOrg
    ){
        Anchor.newBuilder(scanResult)
        .mthod(method)
        .anchorCode(anchorCode)
        .afterSuper(afterOrg)
        .build()
        .generate(project)
    }



    /*static String generateNewOnCreateMethod(String superMethodCode) {
        StringBuilder stringBuilder = new StringBuilder()
        stringBuilder.append(superMethodCode)
        *//*stringBuilder.append("public void onCreate() {\n ")
        stringBuilder.append("super.onCreate();\n")
        stringBuilder.append("}")*//*
        return stringBuilder.toString()
    }*/
    /*private void generateEnabledField(CtClass ctClass, String path) {
        CtField pathCtField = ctClass.declaredFields.find {
            it.name == contentMethod && it.getType().name == "java.lang.String"
        }
        if (pathCtField != null) {
            ctClass.removeField(pathCtField)
        }
        pathCtField = new CtField(classPool.get("java.lang.String"), contentMethod, ctClass)
        pathCtField.setModifiers(Modifier.PRIVATE | Modifier.STATIC)
        ctClass.addField(pathCtField, CtField.Initializer.constant(path))
    }*/


}