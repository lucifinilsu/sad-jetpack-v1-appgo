package com.sad.jetpack.v1.appgo.plugin

import com.sad.jetpack.v1.appgo.annotation.ApplicationLifeCycleAction
import javassist.CtClass
import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.CodeAttribute
import javassist.bytecode.LocalVariableAttribute
import javassist.bytecode.MethodInfo
import org.gradle.api.Project

/*符号	含义
$0, $1, $2, ...	this and 方法的参数
$args	方法参数数组.它的类型为 Object[]
$$	所有实参。例如, m($$) 等价于 m($1,$2,...)
$cflow(...)	cflow 变量
$r	返回结果的类型，用于强制类型转换
$w	包装器类型，用于强制类型转换
$_	返回值
$sig	类型为 java.lang.Class 的参数类型数组
$type	一个 java.lang.Class 对象，表示返回值类型
$class	一个 java.lang.Class 对象，表示当前正在修改的类
*/

class Anchor {
    private Builder builder;
    private Anchor(Builder builder){
        this.builder=builder
    }


    void generate(Project project){

        project.logger.error(">> "+builder.method.name+"'s anchord code is=\n"+builder.anchorCode)
        if (builder.afterSuper){
            builder.method.insertAfter(builder.anchorCode)
        }
        else {
            builder.method.insertBefore(builder.anchorCode)
        }
    }
    static String firstLow(String s){
        String f=s.substring(0,1);
        String e=s.substring(1);
        return f.toLowerCase()+e
    }
    static ArrayList<String> getMethodParametersName(Project project,CtMethod method){
        ArrayList<String> names=new ArrayList<>();
        MethodInfo methodInfo = method.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
                .getAttribute(LocalVariableAttribute.tag);
        if (attr != null) {
            int len = method.getParameterTypes().length;
            // 非静态的成员函数的第一个参数是this
            int pos = Modifier.isStatic(method.getModifiers()) ? 0 : 1;

            for (int i = 0; i < len; i++) {
                String name=attr.variableName(i + pos)
                names.add(name)
                project.logger.error(">> pName is "+name)
            }
        }
        else {
            project.logger.error(">> attr is null")
        }
        return names
    }

    private static void appInitContext(Project project,StringBuilder defaultCode){
        StringBuffer initContextCode=new StringBuffer()
        initContextCode.append("com.sad.jetpack.v1.appgo.api.AppGo.init(this);")
        defaultCode.append(initContextCode.toString())
    }

    static CtMethod getOverwirteMethodFromApplication(Project project,CtClass applicationClass,String methodName,CtClass... parameters){
        CtMethod method = null;
        try{
            method=applicationClass.getDeclaredMethod(methodName,parameters);
        }catch(Exception e){
            project.logger.error(">> "+methodName+" method is not found")
        }

        if (method==null){
            StringBuilder defaultCode=new StringBuilder();
            defaultCode.append("{\nsuper."+methodName+"(\$\$);")
            project.logger.error(">> super  is inited !!!!");
            if ("onCreate".equals(methodName)){
                appInitContext(project,defaultCode)
            }
            defaultCode.append("\n}")

            //方法1
            /*StringBuilder defaultCode=new StringBuilder();
            defaultCode.append("public void "+methodName)
            if (parameters==null || parameters.length>0){
                String ps=""
                String pp=""
                for (int i = 0; i < parameters.length; i++) {
                    CtClass paramter=parameters[i]
                    String pn=firstLow(paramter.getSimpleName())
                    String sp=i==parameters.length-1?"":","
                    String p=paramter.name+" "+pn+sp
                    ps+=p
                    pp+=pn+sp
                }
                defaultCode.append("("+ps+"){super."+methodName+"("+pp+");}")
            }
            else {
                defaultCode.append("(){super."+methodName+"();}")
            }
            method = CtMethod.make(defaultCode.toString(),applicationClass)//此方法返回值的attr属性为null*/


            //方法2

            method = new CtMethod(CtClass.voidType, methodName, parameters, applicationClass);
            method.setModifiers(Modifier.PUBLIC);
            /*if (parameters!=null && parameters.length>0){
                defaultCode.append("{super."+methodName+"(\$\$);}")
            }
            else {
                defaultCode.append("{super."+methodName+"();}")
            }*/
            method.setBody(defaultCode.toString())
            applicationClass.addMethod(method)
            //方法3
           /* try{
                method=applicationClass.getSuperclass().getMethod(methodName,"Landroid/content/res/Configuration;)V");
                applicationClass.addMethod(method)
            }catch(Exception e){
                project.logger.error(">> "+methodName+" method is not found in superClass:"+applicationClass.getSuperclass().name)
            }*/
        }
        else{

        }
        return method
    }

    static ArrayList<CtClass> sort(ArrayList<CtClass> classList,String methodName,String desc){
        ArrayList<CtClass> target=new ArrayList<>(classList)
        Comparator comparator=new Comparator<CtClass>() {
            @Override
            public int compare(CtClass o1, CtClass o2) {
                try {
                    CtMethod method2=o2.getMethod(methodName,desc)
                    CtMethod method1=o1.getMethod(methodName,desc)
                    if (method2==null || method2.getAnnotation(ApplicationLifeCycleAction.class)==null){
                        return -1
                    }
                    if (method1==null || method1.getAnnotation(ApplicationLifeCycleAction.class)==null){
                        return 1
                    }
                    ApplicationLifeCycleAction priority1=method1.getAnnotation(ApplicationLifeCycleAction.class)
                    ApplicationLifeCycleAction priority2=method2.getAnnotation(ApplicationLifeCycleAction.class)
                    return (int) (priority2.priority()-priority1.priority())

                } catch (Exception e) {
                    e.printStackTrace()
                }
                return 0
            }
        };
        Collections.sort(target,comparator)
        return target
    }
    static Builder newBuilder(ClassScanResult scanResult){
        return new Builder(scanResult)
    }
    static class Builder{
        private ClassScanResult scanResult
        private CtMethod method
        private String anchorCode
        private boolean afterSuper
        public Builder(ClassScanResult scanResult){
            this.scanResult=scanResult;
        }
        Builder mthod(CtMethod method){
            this.method=method
            return this
        }

        Builder anchorCode(String anchorCode){
            this.anchorCode=anchorCode
            return this
        }
        Builder afterSuper(boolean afterSuper){
            this.afterSuper=afterSuper;
            return this
        }

        Anchor build(){
            return new Anchor(this)
        }
    }
}