package org.apache.dolphinscheduler.common.context;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;

import java.util.*;

@Slf4j
public class GlobalParametersContext {

    private final static ThreadLocal<Map<String, Object>> parameters = new InheritableThreadLocal<>();

    private static void initParameters() {
        if (parameters.get() == null) {
            parameters.set(new HashMap<>());
        }
    }

    public static void setParameters(Map<String, Object> params) {
        initParameters();
        parameters.get().putAll(params);
    }


    public static void setParameters(String globalParams) {
        TypeReference<List<GlobalParameters>> pType = new TypeReference<List<GlobalParameters>>() {
        };

        List<GlobalParameters> globalParameters = JSONUtils.parseObject(globalParams, pType);
        if  (globalParameters == null) {
            return;
        }
        initParameters();
        for (GlobalParameters globalParameter : globalParameters) {
            parameters.get().put(globalParameter.getProp(), globalParameter.getValue());
        }
        log.info("Inject global parameters to ThreadLocal context.");
    }

    public static void clearParameters() {
        parameters.remove();
        log.info("Remove the global parameters in ThreadLocal context.");
    }

    public static Map<String, Object> getParameters() {
        if (parameters.get() == null) {
            return Collections.emptyMap();
        }
        return parameters.get();
    }
}
