package com.kitap.agent.generate.service;

import com.kitap.agent.util.PropertyReader;
import com.kitap.testresult.dto.agent.GenerationDetails;
import com.kitap.testresult.dto.generate.Clazz;
import com.kitap.testresult.dto.generate.Step;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

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

/**
 * Class used for the parsing of jar file to json
 * @author KT1450
 */
@Slf4j
public class JarParserService{
    URL[] urls;
    URLClassLoader urlClassLoader;
    ClassPool cp = new ClassPool();
    private HashSet<String> listOfClasses = new HashSet<>();
    private Set<Class<?>> listOfTestClasses;
    private Long version;

    private PropertyReader reader = new PropertyReader();

    /**
     * method which provides all the info about classes and packages of jar
     * @param jarFile jar file with class data
     * @param details GenerationDetails object
     * @return list of Clazz objects
     */
    public List<Clazz> getAllClassData(File jarFile, GenerationDetails details) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("method getAllClassData started with jarfile and generationdetails");
        try {
            String jarsPath = details.getProjectPath().getAbsolutePath()+ File.separator + "target" + File.separator + "test-jars";
            File jarDir = new File(jarsPath);
            File[] jarFiles = jarDir.listFiles((dir, name) -> name.endsWith(".jar"));

            assert jarFiles != null;
            urls = new URL[jarFiles.length + 1];
            for (int i = 0; i < jarFiles.length; i++) {
                urls[i] = jarFiles[i].toURI().toURL();
            }
            urls[urls.length - 1] = jarFile.toURI().toURL();
            urlClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

            cp.insertClassPath(jarFile.getAbsolutePath());
            this.version = details.getVersion();
        } catch (IOException e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
        log.info("method getAllClassData completed with returning list of Clazz objects");
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return scanJar(jarFile);
    }

    /**
     * Method scans the input jar file
     * @param jarFile jar file to be scanned
     * @return list of Clazz objects
     */
    private List<Clazz> scanJar(File jarFile) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("scanning the jar by using jarfile as input");
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

            // adding all class names
            if (name.endsWith(".class")) {
                listOfClasses.add(name);
            }
        }
        filterTestClasses();
        log.info("scanning the jar completed with returning list of clazz objects");
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return constructClazzObjects();
    }

    /**
     * Filtering the test classes present in the jar file
     */
    private void filterTestClasses() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("filtering test classes");
        HashSet<Class<?>> classes = new HashSet<>();
        for (String clazz : listOfClasses) {
            if (!clazz.contains("$")) {
                String className = clazz.replaceAll("/", ".").replaceAll(".class", "");
                try {
                    classes.add(urlClassLoader.loadClass(className));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        listOfTestClasses = classes.stream().filter(this::hasMethodWithTestAnnotation).collect(Collectors.toSet());
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
    }

    /**
     * getting list of all classes info
     * @return list of Clazz objects
     */
    private List<Clazz> constructClazzObjects() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("constructing Clazz objects");
        List<Clazz> classes = new ArrayList<>();
        for (Class<?> cls : listOfTestClasses) {
            classes.add(getClassObject(cls));
        }
        log.info("constructed Clazz objects and returning them as list");
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return classes;
    }

    /**
     * getting single class object
     * @param clazz Class object
     * @return Clazz object
     */
    private Clazz getClassObject(Class<?> clazz) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Clazz klazz = new Clazz();
        String name = clazz.getName();
        klazz.setName(name.substring(name.lastIndexOf(".") + 1));
        klazz.setFullyQualifiedName(name);
        klazz.setDescription(name.substring(name.lastIndexOf(".") + 1));
        klazz.setVersion(version);
        klazz.setSteps(constructStepsFromTestMethod(clazz));
        log.info("getting Clazz object");
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return klazz;
    }

    /**
     * getting all the required method formats
     * @param clazz Class object
     * @return list of Step objects
     */
    private List<Step> constructStepsFromTestMethod(Class<?> clazz) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("constructing steps from test method by using clazz object as input");
        //getting declared fields and finding which are steps fields
        Field[] declaredFields = clazz.getDeclaredFields();
        List<String> stepFieldTypes = new ArrayList<>();
        for (Field field : declaredFields) {
            if (isStepField(field))
                stepFieldTypes.add(field.getType().getName());
        }

        // getting single test method which annotated with @Test
        java.lang.reflect.Method testMethod = null;
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            if (isTestMethod(method))
                testMethod = method;
        }
        assert testMethod != null;
        log.info("constructed steps from test method are returned as list");

        // constructing steps for each test script
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return constructSteps(stepFieldTypes, clazz.getName(), testMethod.getName());
    }

    /**
     * constructing steps of a single test method
     * @param stepFieldTypes list of step field types
     * @param className name of the class
     * @param methodName name of the method
     * @return list of step objects
     */
    private List<Step> constructSteps(List<String> stepFieldTypes, String className, String methodName) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<Step> steps = getSteps(className, methodName);
        List<Step> finalSteps = new ArrayList<>();
        long count = 1L;

        for (Step step : steps) {
            String cname = step.getType();
            if (stepFieldTypes.contains(cname)) {
                step.setSequenceNumber(count++);
                finalSteps.add(step);
            }
        }
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return finalSteps;
    }

    /**
     * getting steps of a test method
     * @param className name of the class
     * @param methodName name of the method
     * @return list of step objects
     */
    private List<Step> getSteps(String className, String methodName) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        CtClass ctClass;
        List<Step> steps = new ArrayList<>();
        try {
            ctClass = cp.get(className);
            CtMethod method = ctClass.getDeclaredMethod(methodName);
            method.instrument(
                    new ExprEditor() {
                        public void edit(MethodCall m) {
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
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return steps;
    }

    /**
     * Checking whether test annotation present at method level
     * @param testClass test Class object
     * @return true if present else false
     */
    private boolean hasMethodWithTestAnnotation(final Class<?> testClass) {
        return stream(testClass.getDeclaredMethods()).anyMatch(this::isTestMethod);
    }

    /**
     * Checking input method is test method or not
     * @param method Method object
     * @return true if test method else false
     */
    private boolean isTestMethod(final java.lang.reflect.Method method) {
        return containsAnnotationCalled(method.getAnnotations(), "Test");
    }

    /**
     * Checking input field is step field or not
     * @param field Field object
     * @return true if field else false
     */
    private boolean isStepField(final Field field) {
        return containsAnnotationCalled(field.getAnnotations(), "Steps");
    }

    /**
     * Checking for called annotation
     * @param annotations array of annotations
     * @param annotationName name of the annotation
     * @return true if matched else false
     */
    private boolean containsAnnotationCalled(java.lang.annotation.Annotation[] annotations, String annotationName) {
        return stream(annotations).anyMatch(annotation -> annotation.annotationType().getSimpleName().equals(annotationName));
    }


}
