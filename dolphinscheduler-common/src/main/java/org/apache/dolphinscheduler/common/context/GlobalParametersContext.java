package org.apache.dolphinscheduler.common.context;

import org.apache.dolphinscheduler.common.utils.JSONUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
public class GlobalParametersContext {

    private final static ThreadLocal<List<GlobalParameters>> parameters = new InheritableThreadLocal<>();

    public static void setParameters(String globalParams) {
        TypeReference<List<GlobalParameters>> pType = new TypeReference<List<GlobalParameters>>() {
        };

        List<GlobalParameters> globalParameters = JSONUtils.parseObject(globalParams, pType);
        parameters.set(globalParameters);

        log.info("Inject global parameters to ThreadLocal context.");
    }

    public static void clearParameters() {
        parameters.remove();
        log.info("Remove the global parameters in ThreadLocal context.");
    }

    public static Object getParameter(String parameterName) {
        List<GlobalParameters> globalParameters = parameters.get();
        for (GlobalParameters globalParameter : globalParameters) {
            if (Objects.equals(globalParameter.getProp(), parameterName)) {
                return globalParameter.getValue();
            }
        }
        return null;
    }

    public static Map<String, Object> getParameters() {
        List<GlobalParameters> globalParameters = parameters.get();
        if (globalParameters == null) {
            return Collections.emptyMap();
        }
        return globalParameters.stream().collect(Collectors.toMap(
                GlobalParameters::getProp, GlobalParameters::getValue));
    }
}
