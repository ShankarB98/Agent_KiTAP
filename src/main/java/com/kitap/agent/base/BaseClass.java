package com.kitap.agent.base;

import com.kitap.agent.ui.machineInfo.MachineInformation;
import com.kitap.agent.util.PropertyReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@Slf4j
public class BaseClass{

    /**
     * This class will do loading properties from Property reader class and holds those values in a variable
     * And provides information when ever requested instead of reading values from application.properties file
     * all the time.
     * Holds information and references of classes and information which required over the project.
     * */

    static final public String separator = File.separator;
    static final public PropertyReader reader = new PropertyReader();
    static final public Properties properties = reader.loadProperties();
    static final public MachineInformation machineInformation = new MachineInformation();

    /**
     * @Description returns single property from already loaded properties
     * @return String
     * */
    public static String getProperty(String propertyName){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("getting property by using propertyName as input");
        propertyName = Objects.requireNonNullElse(propertyName, "");
        if (propertyName.equals("")){
            log.error("invalid property name #"+ propertyName);
            throw new RuntimeException("please enter valid property name");
        }
        log.info("returning the property value");
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return properties.getProperty(propertyName);
    }

    /**
     * @Description returns list of properties from already loaded properties
     * @return List of Strings
     * */
    public static List<String> getProperties(String[] propertyNames){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("getting properties by using array of propertyNames as input");
        List<String> properties = new ArrayList<>();
        for (String propertyName: propertyNames){
            String value = BaseClass.properties.getProperty(propertyName);
            if (value != null && !value.equals("")) {
                properties.add(value);
            }else{
                log.error("invalid property name #"+ propertyName);
                throw new RuntimeException("please enter valid property name");
            }
        }
        log.info("returning the list of properties");
        stopWatch.stop();
        log.info("Execution time for "+new Object(){}.getClass().getEnclosingMethod().getName()+
                " method is "+String.format("%.2f",stopWatch.getTotalTimeSeconds())+" seconds");
        return properties;
    }
}
