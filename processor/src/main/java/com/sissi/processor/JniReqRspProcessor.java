package com.sissi.processor;

import com.google.auto.service.AutoService;
import com.sissi.annotation.GenFile;
import com.sissi.annotation.ReqDef;
import com.sissi.annotation.Request;
import com.sissi.annotation.RspClazz;
import com.sissi.annotation.RspDef;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;

/**
 * Created by Sissi on 2018/9/3.
 */

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "com.sissi.annotation.ReqDef",
        "com.sissi.annotation.RspDef",
        "com.sissi.annotation.Request",
        "com.sissi.annotation.RspClazz",
        "com.sissi.annotation.GenFile",
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class JniReqRspProcessor extends AbstractProcessor {
//    private static final String GENERATED_PACKAGE_NAME = "com.sissi.lib";
//    private static final String GENERATED_CLASS_NAME = "ReqRspMap$$FromAnnotation";
    private static final String ACCEPT_REQ_ENCLOSING_ELEMENT_NAME = "com.sissi.pluggableannotationprocessordemo.EmReq";
    private static final String ACCEPT_RSP_ENCLOSING_ELEMENT_NAME = "com.sissi.pluggableannotationprocessordemo.EmRsp";
    private static boolean bDone = false;

    private Map<String, String[]> reqRspsMap = new HashMap<>();

    private Map<String, Integer> reqTimeoutMap = new HashMap<>();

    private Map<String, String> rspClazzMap = new HashMap<>();

    private String packageName;

    private String className;

    private Messager messager;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE, "############# IN");

        if (bDone){
            return true;
        }
        bDone = true;

        if (collectInfo(roundEnvironment)) {
            generateFile();
        }

        messager.printMessage(Diagnostic.Kind.NOTE, "############# OUT");

        return true;
    }

    private boolean collectInfo(RoundEnvironment roundEnvironment){
        reqRspsMap.clear();
        reqTimeoutMap.clear();
        rspClazzMap.clear();

        Set<? extends Element> genFileElements = roundEnvironment.getElementsAnnotatedWith(GenFile.class);
        TypeElement genFileElement = null;
        GenFile genFileAnno = null;
        Iterator genFileIt = genFileElements.iterator();
        while (genFileIt.hasNext()){
            genFileElement = (TypeElement) genFileIt.next();
            genFileAnno = genFileElement.getAnnotation(GenFile.class);
            packageName = genFileAnno.packageName();
            className = genFileAnno.className();
            if (null != packageName && null != className) {
                break;
            }
        }

        if (null == packageName || null == className) {
            return false;
        }

        Set<? extends Element> reqDefinations = roundEnvironment.getElementsAnnotatedWith(ReqDef.class);
        Set<? extends Element> rspDefinations = roundEnvironment.getElementsAnnotatedWith(RspDef.class);
        TypeElement reqDefination = null;
        TypeElement rspDefination = null;
        ReqDef reqDef = null;

        Class rspDefClass;
        Iterator reqIt = reqDefinations.iterator();
        Iterator rspIt = rspDefinations.iterator();
        String ResponseDefinationFullClassName;
        boolean found = false;
        // 遍历所有带RequestDefination注解的类,获取其ResponseDefination属性值V,
        // 遍历所有带ResponseDefination注解的类,看是否存在完整类名匹配V的类,若存在则成功匹配结束,若不存在则返回失败
        while (reqIt.hasNext() && !found){
            reqDefination = (TypeElement) reqIt.next();

            reqDef = reqDefination.getAnnotation(ReqDef.class);
            try {
                rspDefClass = reqDef.rspDefClass();
                ResponseDefinationFullClassName = rspDefClass.getCanonicalName();
            }catch (MirroredTypeException mte) {
                DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                ResponseDefinationFullClassName = classTypeElement.getQualifiedName().toString();
                messager.printMessage(Diagnostic.Kind.NOTE, "rspDefClass.name="+ResponseDefinationFullClassName);
            }

            while (rspIt.hasNext()){
                rspDefination = (TypeElement) rspIt.next();
                messager.printMessage(Diagnostic.Kind.NOTE, "rspDefination.name="+rspDefination.getQualifiedName().toString());
                if (rspDefination.getQualifiedName().toString().equals(ResponseDefinationFullClassName)){
                    found = true;
                    break;
                }
            }
        }

        if (!found){
            return false;
        }

        List<? extends Element> reqElements = reqDefination.getEnclosedElements();
        Request request;
        for (Element reqElement : reqElements){
            if (ElementKind.ENUM_CONSTANT != reqElement.getKind()){
                continue;
            }
            request = reqElement.getAnnotation(Request.class);
            messager.printMessage(Diagnostic.Kind.NOTE, "reqElement.getSimpleName()="+reqElement.getSimpleName()+ "  request.rspSeq()="+ request.rspSeq()+ "  request.timeout()="+ request.timeout());
            reqRspsMap.put(reqElement.getSimpleName().toString(), request.rspSeq());
            reqTimeoutMap.put(reqElement.getSimpleName().toString(), request.timeout());
        }

        List<? extends Element> rspElements = rspDefination.getEnclosedElements();
        RspClazz rspClazz;
        String rspClazzFullName;
        for (Element rspElement : rspElements){
            if (ElementKind.ENUM_CONSTANT != rspElement.getKind()){
                continue;
            }
            rspClazz = rspElement.getAnnotation(RspClazz.class);
            try {
                Class clz = rspClazz.value();
                rspClazzFullName = clz.getCanonicalName();
            }catch (MirroredTypeException mte) {
                DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                rspClazzFullName = classTypeElement.getQualifiedName().toString();
            }
            messager.printMessage(Diagnostic.Kind.NOTE, "reqElement.getSimpleName()="+rspElement.getSimpleName()+ " rspClazzFullName="+rspClazzFullName);
            rspClazzMap.put(rspElement.getSimpleName().toString(), rspClazzFullName);
        }
//
//        Set<? extends Element> reqElements = roundEnvironment.getElementsAnnotatedWith(Request.class);
//        Element enclosingElement;
//        TypeElement acceptEnclosingElement;
//        for (Element req : reqElements){
//            enclosingElement = req.getEnclosingElement();
//            acceptEnclosingElement = (TypeElement) enclosingElement;
//            messager.printMessage(Diagnostic.Kind.NOTE, "acceptEnclosingElement.getQualifiedName()="+acceptEnclosingElement.getQualifiedName());
//            if (!acceptEnclosingElement.getQualifiedName().contentEquals(ACCEPT_REQ_ENCLOSING_ELEMENT_NAME)){
//                continue;
//            }
//
//            messager.printMessage(Diagnostic.Kind.NOTE, "req.getSimpleName()="+req.getSimpleName()+" req.asType()="+req.asType());
//
//            Request request = req.getAnnotation(Request.class);
//            for (String rsp: request.rspSeq()){
//                messager.printMessage(Diagnostic.Kind.NOTE, "rspSeq="+rsp);
//            }
//
//            messager.printMessage(Diagnostic.Kind.NOTE, "request.rspSeq()="+request.rspSeq()+" request.timeout()="+request.timeout());
//
//            reqRspsMap.put(req.getSimpleName().toString(), request.rspSeq());
//            reqTimeoutMap.put(req.getSimpleName().toString(), request.timeout());
//        }
//
//
//        Set<? extends Element> rspElements = roundEnvironment.getElementsAnnotatedWith(RspClazz.class);
//        for (Element rsp : rspElements) {
//            enclosingElement = rsp.getEnclosingElement();
//            acceptEnclosingElement = (TypeElement) enclosingElement;
//            messager.printMessage(Diagnostic.Kind.NOTE, "acceptEnclosingElement.getQualifiedName()=" + acceptEnclosingElement.getQualifiedName());
//            if (!acceptEnclosingElement.getQualifiedName().contentEquals(ACCEPT_RSP_ENCLOSING_ELEMENT_NAME)) {
//                continue;
//            }
//
//            messager.printMessage(Diagnostic.Kind.NOTE, "rsp.getSimpleName()=" + rsp.getSimpleName());
//
//            TypeMirror typeMirror = rsp.asType();
//            messager.printMessage(Diagnostic.Kind.NOTE, "typeMirror.getClass()=" + typeMirror.getKind());
//            RspClazz rspClazz = rsp.getAnnotation(RspClazz.class);
//            try {
//                rspClazzMap.put(rsp.getSimpleName().toString(), rspClazz.value());
//                messager.printMessage(Diagnostic.Kind.NOTE, "rspClazz.value()=" + rspClazz.value());
//            } catch (MirroredTypeException mte) {
//                messager.printMessage(Diagnostic.Kind.NOTE, "mte.getTypeMirror()=" + mte.getTypeMirror());
//                TypeName typeName = TypeName.get(mte.getTypeMirror());
//                messager.printMessage(Diagnostic.Kind.NOTE, "typeName.getClass()=" + typeName);
//
////                rspClazzMap.put(rsp.getSimpleName().toString(), mte.getTypeMirror());
////                DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
////                TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
////                qualifiedSuperClassName = classTypeElement.getQualifiedName().toString();
////                simpleTypeName = classTypeElement.getSimpleName().toString();
//            }
//
////            rspClazzMap.put(rsp.getSimpleName().toString(), rspClazz.value());
//
//        }


//        // Utility elements that come with javax.annotation.processing
//        Elements elements = processingEnv.getElementUtils();
//        Types types = processingEnv.getTypeUtils();
//
//// These are elements, they never have generic types associated
//        TypeElement listElement = elements.getTypeElement(List.class.getName());
//        TypeElement strElement  = elements.getTypeElement(String.class.getName());
//
//// Build a Type: List<String>
//        DeclaredType listStrType = types.getDeclaredType(
//                listElement,
//                strElement.asType());
//
//// Build a Type: List<List<String>>
//        DeclaredType listlistStrType = types.getDeclaredType(
//                listElement,
//                listStrType
//                );
////
////
//        messager.printMessage(Diagnostic.Kind.NOTE, "listElement=" + listElement + " strElement="+strElement
//                + " listStrType="+listStrType+ " listlistStrType="+listlistStrType);


        return true;
    }

    private void generateFile(){
        String fieldNameReqRspsMap = "reqRspsMap";
        String fieldNameReqTimeoutMap = "reqTimeoutMap";
        String fieldNameRspClazzMap = "rspClazzMap";
        String methodNameReqRspsMapGetter = "getReqRspsMap";
        String methodNameReqTimeoutMapGetter = "getReqTimeoutMap";
        String methodNameRspClazzMapGetter = "getRspClazzMap";
        String emReq = "EmReq";
        String emRsp = "EmRsp";

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
//        constructor.addStatement("$L = new $T<>()", fieldNameReqRspsMap, HashMap.class)
//                .addStatement("$L = new $T<>()", fieldNameReqTimeoutMap, HashMap.class);


        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder()
                .addStatement("$L = new $T<>()", fieldNameReqRspsMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameReqTimeoutMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameRspClazzMap, HashMap.class)
                ;

        for(String req : reqRspsMap.keySet()){
            StringBuffer value = new StringBuffer();
            String[] rspSeq = reqRspsMap.get(req);
            for (String rsp : rspSeq){
                value.append("\""+rsp+"\", ");
            }
            codeBlockBuilder.addStatement("$L.put($S, new String[]{$L})", fieldNameReqRspsMap, req, value);
        }

        for(String req : reqTimeoutMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L)", fieldNameReqTimeoutMap, req, reqTimeoutMap.get(req));
        }

        for(String rsp : rspClazzMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L.class)", fieldNameRspClazzMap, rsp, rspClazzMap.get(rsp));
        }

//        constructor.addCode(codeBlockBuilder.build());

        MethodSpec.Builder reqRspsMapGetter = MethodSpec.methodBuilder(methodNameReqRspsMapGetter)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return $L", fieldNameReqRspsMap)
                .returns(ParameterizedTypeName.get(Map.class, String.class, String[].class));

        MethodSpec.Builder reqTimeoutMapGetter = MethodSpec.methodBuilder(methodNameReqTimeoutMapGetter)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return $L", fieldNameReqTimeoutMap)
                .returns(ParameterizedTypeName.get(Map.class, String.class, Integer.class));

        MethodSpec.Builder rspClazzMapGetter = MethodSpec.methodBuilder(methodNameRspClazzMapGetter)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return $L", fieldNameRspClazzMap)
                .returns(ParameterizedTypeName.get(Map.class, String.class, Class.class));

        // 构建Class
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
//                .addField(FieldSpec.builder(int.class, "baseInt").build())
//                .addField(FieldSpec.builder(int[].class, "intArray").build())
//                .addField(FieldSpec.builder(ParameterizedTypeName.get(List.class, String.class), "list").build())
//                .addField(FieldSpec.builder(ParameterizedTypeName.get(List.class, List.class), "nestedList").build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String[].class), fieldNameReqRspsMap, Modifier.PRIVATE, Modifier.STATIC).build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Integer.class), fieldNameReqTimeoutMap, Modifier.PRIVATE, Modifier.STATIC).build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Class.class), fieldNameRspClazzMap, Modifier.PRIVATE, Modifier.STATIC).build())
//                .addField(FieldSpec.builder(ParameterizedTypeName.get(Set.class, String.class), "set").build())
//                .addField(FieldSpec.builder(TypeVariableName.get("T"), "t").build())
//                .addField(FieldSpec.builder(WildcardTypeName.subtypeOf(String.class), "wildcardType").build())
                .addStaticBlock(codeBlockBuilder.build())
                .addMethod(constructor.build())
                .addMethod(reqRspsMapGetter.build())
                .addMethod(reqTimeoutMapGetter.build())
                .addMethod(rspClazzMapGetter.build())
                .build();

        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .build();

        // 生成class文件
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
