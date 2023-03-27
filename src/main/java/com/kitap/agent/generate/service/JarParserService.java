package com.kitap.agent.generate.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kitap.testresult.dto.generate.Clazz;
import com.kitap.testresult.dto.generate.Step;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static java.util.Arrays.stream;

@Slf4j
public class JarParserService extends URLClassLoader{

    JarFile jarFile1;
    URL url;
    URL[] urls;
    ClassLoader cl;

    ClassPool cp = new ClassPool();

    private HashSet<String> listOfClasses = new HashSet<>();
    private Set<Class<?>> listOfTestClasses;

    private Long version;
    private String autName;


    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final URL[] urlss = {};
    public JarParserService() {
        super(urlss);
    }

    /** method which provides all the info about classes and packages of jar */
    public List<Clazz> getAllClassData(File jarFile, String autName, Long version){
        try {
            url = jarFile.toURI().toURL();
            urls = new URL[]{url};
            cl = new URLClassLoader(urls);
            cp.insertClassPath(jarFile.getAbsolutePath());
            this.version = version;
            this.autName = autName;
        }catch(IOException e){
            log.error(e.toString());
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
        return scanJar(jarFile);
    }

    private List<Clazz> scanJar(File jarFile) {
        JarFile jar;
        try {
            jar = new JarFile(jarFile);
        } catch (IOException e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        }
        Enumeration<? extends JarEntry> enumeration = jar.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            String name = zipEntry.getName();

            /** adding all class names*/
            if(name.endsWith(".class")){
                listOfClasses.add(name);
            }
        }
        filterTestClasses1();
        return constructClazzObjects();
    }

    /** the following method filters and provides the classes of test cases */
    private void filterTestClasses(){
        HashSet<Class<?>> classes = new HashSet<>();
        for (String clazz: listOfClasses){
            String className = clazz.replaceAll("/", ".").replaceAll(".class","");
            try {
                classes.add(cl.loadClass(className));
            } catch (ClassNotFoundException e) {
                log.error(e.toString());
                throw new RuntimeException(e);
            }
        }
        listOfTestClasses = classes.stream().filter(this::hasMethodWithTestAnnotation).collect(Collectors.toSet());
    }

    private void filterTestClasses1(){
        HashSet<Class<?>> classes = new HashSet<>();
        for (String clazz: listOfClasses){
            if(!clazz.contains("$")) {
                String className = clazz.replaceAll("/", ".").replaceAll(".class", "");
                String existsTest = className.substring(className.lastIndexOf("."));
                if(existsTest.contains("test")|| existsTest.contains("Test")){
                    try {
                        classes.add(cl.loadClass(className));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        listOfTestClasses = classes.stream().filter(this::hasMethodWithTestAnnotation).collect(Collectors.toSet());
    }

    /** getting list of all classes info */
    private List<Clazz> constructClazzObjects(){
        List<Clazz> classes = new ArrayList<>();
        for(Class<?> cls : listOfTestClasses){
            classes.add(getClassObject(cls));
        }
        return classes;
    }

    /** getting single class object */
    private Clazz getClassObject(Class<?> clazz){
        Clazz klazz = new Clazz();
        String name = clazz.getName();
        klazz.setName(name.substring(name.lastIndexOf(".")+1));
        klazz.setFullyQualifiedName(name);
        klazz.setDescription(name.substring(name.lastIndexOf(".")+1));
        klazz.setVersion(version);
        klazz.setSteps(constructStepsFromTestMethod(clazz));
        return klazz;
    }

    /** getting all the required method formats */
    private List<Step> constructStepsFromTestMethod(Class<?> clazz){
        /** getting declared fields and finding which are steps fields */
        Field[] declaredFields = clazz.getDeclaredFields();
        List<String> stepFieldTypes = new ArrayList<>();
        for (Field field: declaredFields){
            if(isStepFiled(field))
                stepFieldTypes.add(field.getType().getName());
        }

        /** getting single test method which annotated with @Test */
        java.lang.reflect.Method testMethod = null;
        for (java.lang.reflect.Method method: clazz.getDeclaredMethods()){
            if (isTestMethod(method))
                testMethod = method;
        }
        assert testMethod != null;

        /** constructing steps for each test script */
        return constructSteps(stepFieldTypes, clazz.getName(), testMethod.getName());
    }

    /** constructing steps of a single test method */
    private List<Step> constructSteps(List<String> stepFieldTypes, String className, String methodName){
        List<Step> steps = getSteps(className, methodName);
        List<Step> finalSteps = new ArrayList<>();
        long count = 1L;

        for (Step step: steps){
            String cname = step.getType();
            if(stepFieldTypes.contains(cname)){
                step.setSequenceNumber(count++);
                finalSteps.add(step);
            }
        }
        return finalSteps;
    }

    private List<Step> getSteps(String className, String methodName){
        CtClass ctClass = null;
        List<Step> steps = new ArrayList<>();
        try {
            ctClass = cp.get(className);
            CtMethod method = ctClass.getDeclaredMethod(methodName);
            method.instrument(
                    new ExprEditor() {
                        public void edit(MethodCall m) throws CannotCompileException
                        {
                            String name = m.getMethodName();
                            Step step = new Step();
                            step.setName(name);
                            step.setDescription(name);
                            step.setType(m.getClassName());
                            steps.add(step);
                        }
                    });
        } catch (NotFoundException | CannotCompileException e) {
            throw new RuntimeException(e);
        }
        return steps;
    }


    /** getting all the annotations on methods*/
    /*private List<Annotation> getAnnotations(java.lang.reflect.Method[] methods){
        List<Annotation> annotations = new ArrayList<>();
        for (java.lang.reflect.Method method: methods) {
            java.lang.annotation.Annotation[] arr = method.getAnnotations();
            if (arr.length > 1){
                Annotation annoBean = new Annotation();
                for(java.lang.annotation.Annotation anno:  arr){
                    annoBean.setName(anno.toString());
                    annoBean.setMethodName(method.getName());
                    annotations.add(annoBean);
                }
            }
        }
        return annotations;
    }*/

    private boolean hasMethodWithTestAnnotation(final Class<?> testClass) {
        return stream(testClass.getDeclaredMethods()).anyMatch(this::isTestMethod);
    }
    private boolean isTestMethod(final java.lang.reflect.Method method) {
        return containsAnnotationCalled(method.getAnnotations(), "Test");
    }

    private boolean isStepFiled(final Field field){
        return containsAnnotationCalled(field.getAnnotations(), "Steps");
    }
    private boolean containsAnnotationCalled(java.lang.annotation.Annotation[] annotations, String annotationName) {
        return stream(annotations).anyMatch(annotation -> annotation.annotationType().getSimpleName().equals(annotationName));
    }


}
