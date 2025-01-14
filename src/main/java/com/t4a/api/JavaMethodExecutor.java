package com.t4a.api;

import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Type;
import com.google.gson.Gson;
import com.t4a.action.ExtendedInputParameter;
import com.t4a.action.ExtendedPredictedAction;
import com.t4a.action.http.HttpPredictedAction;
import com.t4a.action.http.InputParameter;
import com.t4a.action.shell.ShellPredictedAction;
import com.t4a.predict.LoaderException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is one of the main classes which is part of processing logic. IT does alll the mapping of actions to
 * predicted options and creates the instance of action class. It is also responsible for invoking the correct action
 * and pass back the response.
 */
@Slf4j
public class JavaMethodExecutor extends JavaActionExecutor {
    private final Map<String, Object> properties = new HashMap<>();
    private FunctionDeclaration generatedFunction;
    private Gson gson = new Gson();

    private Class<?> clazz ;
    private Method method;
    private AIAction action;
    public JavaMethodExecutor() {

    }

    public JavaMethodExecutor(Gson gson) {
        this.gson = gson;

    }










    public Map<String, Object> getProperties() {
        return properties;
    }

    public FunctionDeclaration getGeneratedFunction() {
        return generatedFunction;
    }

    public Gson getGson() {
        return gson;
    }

    private FunctionDeclaration buildFunction(String className, String methodName, String funName, String discription) {
        mapMethod(className, methodName);
        generatedFunction = getBuildFunction(funName, discription);
        return generatedFunction;
    }
    private FunctionDeclaration buildFunction(HttpPredictedAction action,  String funName, String discription) {
        if(action.isHasJson()) {
            if (discription == null)
                discription = funName;
            generatedFunction = getBuildFunction(action.getJsonMap(),funName,discription);
        }
        else {
            mapMethod(action);
            if (discription == null)
                discription = funName;
            generatedFunction = getBuildFunction(funName, discription);
        }

        return generatedFunction;
    }
    private FunctionDeclaration buildFunction(ExtendedPredictedAction action,  String funName, String discription) {
        mapMethod(action);
        generatedFunction = getBuildFunction(funName, discription);
        return generatedFunction;
    }
    private FunctionDeclaration buildFunction(ShellPredictedAction action, String funName, String discription) {
        mapMethod(action);
        generatedFunction = getBuildFunction(funName, discription);
        return generatedFunction;
    }

    /**
     * Take the AIAction class and based on the type it returns a FunctionDeclaration for Gemini
     * @param action
     * @return
     */
    public FunctionDeclaration buildFunction(AIAction action) {
        this.action = action;
        if(action.getActionType().equals(ActionType.SHELL)) {
            ShellPredictedAction shellAction = (ShellPredictedAction)action;
            return buildFunction(shellAction,action.getActionName(),action.getDescription());
        } else if(action.getActionType().equals(ActionType.HTTP)) {
            HttpPredictedAction httpAction = (HttpPredictedAction)action;
            return buildFunction(httpAction,httpAction.getActionName(),httpAction.getDescription());
        } else if(action.getActionType().equals(ActionType.EXTEND))  {
            ExtendedPredictedAction extendedPredictedAction = (ExtendedPredictedAction)action;
            return buildFunction(extendedPredictedAction,extendedPredictedAction.getActionName(),extendedPredictedAction.getDescription());
        }
        else if(action.getActionType().equals(ActionType.JAVAMETHOD)) {
            JavaMethodAction methodAction = (JavaMethodAction)action;
            if(!methodAction.isComplexMethod()) {
                return buildFunction(action.getClass().getName(), action.getActionName(), action.getActionName(), action.getDescription());
            } else {
                log.warn("method has pojos or complex data type , Will try to convert them, if it fails  please use GeminiPromptTransformer");
                return buildFunction(action.getClass().getName(), action.getActionName(), action.getActionName(), action.getDescription());
            }
        } else
            return buildFunction(action.getClass().getName(), action.getActionName(), action.getActionName(), action.getDescription());
    }
    public void mapMethod(HttpPredictedAction action) {
        List<InputParameter> inputParameterList =action.getInputObjects();
        for (InputParameter parameter : inputParameterList) {
            if(!parameter.hasDefaultValue())
              properties.put(parameter.getName(), mapType(parameter.getType()));
        }

    }

    public void mapMethod(AIAction action) {
        this.action = action;
        if(action.getActionType().equals(ActionType.SHELL)) {
            ShellPredictedAction shellAction = (ShellPredictedAction)action;
            mapMethod(shellAction);
        } else if(action.getActionType().equals(ActionType.HTTP)) {
            HttpPredictedAction httpAction = (HttpPredictedAction)action;
            if(httpAction.isHasJson()) {
                //covert Json to properties
                Map<String,Object> map = httpAction.getJsonMap();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    properties.put(key, mapType(value.getClass()));
                }

            }
            else {
                mapMethod(httpAction);
            }
        } else if(action.getActionType().equals(ActionType.EXTEND))  {
            ExtendedPredictedAction extendedPredictedAction = (ExtendedPredictedAction)action;
            mapMethod(extendedPredictedAction);
        } else if(action.getActionType().equals(ActionType.JAVAMETHOD)){
            mapMethod(action.getClass().getName(),action.getActionName());
        }
    }
    private void mapMethod(ExtendedPredictedAction action) {
        List<ExtendedInputParameter> inputParameterList =action.getInputParameters();
        for (ExtendedInputParameter parameter : inputParameterList) {
            if(!parameter.hasDefaultValue())
                properties.put(parameter.getName(), mapType(parameter.getType()));
        }

    }
    private void mapMethod(ShellPredictedAction action) {
        String nameList =action.getParameterNames();
        String[] names = nameList.split(",");
        for (String name : names) {
                properties.put(name, mapType("String"));
        }

    }

    /**
     * Convert method to map with name and value ( needs --parameter to be set at compiler to work )
     * @param className
     * @param methodName
     */

    private void mapMethod(String className, String methodName) {


        try {
            clazz = Class.forName(className);
            Method[] methods = clazz.getMethods();

            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    this.method = method;
                    Parameter[] parameters = method.getParameters();

                    for (int i = 0; i < parameters.length; i++) {
                        Type tp = mapType(parameters[i].getType());
                        if(tp == Type.OBJECT) {
                            properties.put(parameters[i].getName(),parameters[i].getType());
                        } else {
                            properties.put(parameters[i].getName(), tp);
                        }
                    }

                    log.debug("Method arguments for " + methodName + ": " + properties);
                    return;
                }
            }

            log.error("Method not found: " + methodName);
        } catch (ClassNotFoundException e) {
            log.error("Class not found: " + className);
        }
    }

    public AIAction getAction() {
        return action;
    }

    /**
     * This method invokes the action based on the type of the action. It gets the values for the input params
     * from the prompt and populates it for the action
     * @param response
     * @param instance
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */

    public Object action(GenerateContentResponse response, AIAction instance) throws InvocationTargetException, IllegalAccessException {

        Map<String, Object> propertyValuesMap = getPropertyValuesMap(response);
        if(instance.getActionType().equals(ActionType.HTTP)) {
         HttpPredictedAction action = (HttpPredictedAction) instance;
         if(propertyValuesMap.keySet().size() <1) {
             propertyValuesMap = getPropertyValuesMapMap(response);
         }
            try {
                 return action.executeHttpRequest(propertyValuesMap);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else  if(instance.getActionType().equals(ActionType.SHELL)) {
            ShellPredictedAction action = (ShellPredictedAction) instance;
            try {
                String paramNamesStr = action.getParameterNames();
                String[] paramNamesArray = paramNamesStr.split(",");
                String[] paraNamesToPassToShell = new String[paramNamesArray.length];
                for (int i = 0; i < paramNamesArray.length; i++) {
                    String s = paramNamesArray[i];
                    paramNamesArray[i] = (String)propertyValuesMap.get(s);
                    log.debug(paramNamesArray[i]);

                }
                action.executeShell(paramNamesArray);
                return "Executed "+action.getActionName();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } if(instance.getActionType().equals(ActionType.EXTEND)) {
            ExtendedPredictedAction action = (ExtendedPredictedAction) instance;
            try {
                return action.extendedExecute(propertyValuesMap);

            } catch (LoaderException e) {
                throw new RuntimeException(e);
            }
        } else {
            Parameter[] parametersFromMethod = method.getParameters();

            // Create an array to hold the parameter values
            Object[] parameterValues = new Object[parametersFromMethod.length];


            // Populate the parameter values from the map based on parameter names
            for (int i = 0; i < parametersFromMethod.length; i++) {
                if(mapType(parametersFromMethod[i].getType()) == Type.OBJECT) {
                    log.debug(parametersFromMethod[i].getType()+"");
                    parameterValues[i]=  getGson().fromJson(getGson().toJson(getPropertyValuesMapMap(response).get(parametersFromMethod[i].getName())),parametersFromMethod[i].getType());
                    log.debug("created "+parameterValues[i].toString());
                } else {
                    parameterValues[i] = propertyValuesMap.get(parametersFromMethod[i].getName());
                }
            }


            // Invoke the method with arguments
            Object obj = null;
            try {
                obj = method.invoke(instance, parameterValues);
            }catch (Exception e) {
                log.warn("could not invoke method returning values "+e.getMessage());
            }
            if(obj == null) {
                obj = "{failed}";
            }
            return obj;
        }
    }
    public Object action(Object[] params, AIAction instance) {
        Object obj = null;
        try {
            obj = method.invoke(instance, params);
        }catch (Exception e) {
            log.warn("could not invoke method returning values"+e.getMessage());
        }
        if(obj == null) {
            obj = "{failed}";
        }
        return obj;
    }

    public Object action(String params, AIAction instance) throws InvocationTargetException, IllegalAccessException {

        Map<String, Object> propertyValuesMap = getPropertyValuesMap(params);
        if(instance.getActionType().equals(ActionType.HTTP)) {
            HttpPredictedAction action = (HttpPredictedAction) instance;

            try {
                return action.executeHttpRequest(propertyValuesMap);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else  if(instance.getActionType().equals(ActionType.SHELL)) {
            ShellPredictedAction action = (ShellPredictedAction) instance;
            try {
                String paramNamesStr = action.getParameterNames();
                String[] paramNamesArray = paramNamesStr.split(",");
                String[] paraNamesToPassToShell = new String[paramNamesArray.length];
                for (int i = 0; i < paramNamesArray.length; i++) {
                    String s = paramNamesArray[i];
                    paramNamesArray[i] = (String)propertyValuesMap.get(s);
                    log.debug(paramNamesArray[i]);

                }
                action.executeShell(paramNamesArray);
                return "Executed "+action.getActionName();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } if(instance.getActionType().equals(ActionType.EXTEND)) {
            ExtendedPredictedAction action = (ExtendedPredictedAction) instance;
            try {
                return action.extendedExecute(propertyValuesMap);

            } catch (LoaderException e) {
                throw new RuntimeException(e);
            }
        } else {
            String[] parameterNames = Arrays.stream(method.getParameters())
                    .map(p -> p.getName())
                    .toArray(String[]::new);

            // Create an array to hold the parameter values
            Object[] parameterValues = new Object[parameterNames.length];

            // Populate the parameter values from the map based on parameter names
            for (int i = 0; i < parameterNames.length; i++) {
                parameterValues[i] = propertyValuesMap.get(parameterNames[i]);
            }


            // Invoke the method with arguments
            Object obj = null;
            try {
                obj = method.invoke(instance, parameterValues);
            }catch (Exception e) {
                log.warn("could not invoke method returning values"+e.getMessage());
            }
            if(obj == null) {
                obj = "{failed}";
            }
            return obj;
        }
    }

}
