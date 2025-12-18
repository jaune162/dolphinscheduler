package org.apache.dolphinscheduler.common.context;

import lombok.Data;

@Data
public class GlobalParameters {

    private String prop;

    private String direct;

    private String type;

    private Object value;

}
