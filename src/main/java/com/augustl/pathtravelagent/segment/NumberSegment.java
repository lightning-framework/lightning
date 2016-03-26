package com.augustl.pathtravelagent.segment;

import com.augustl.pathtravelagent.RouteMatchResult;

public class NumberSegment implements IParametricSegment {
    private final String paramName;

    public NumberSegment(String paramName) {
        this.paramName = paramName;
    }

    @Override
    public String getParamName() {
        return this.paramName;
    }

    @Override
    public RouteMatchResult.IResult getValue(String rawValue) {
        try {
            Integer parsed = Integer.parseInt(rawValue, 10);
            return new RouteMatchResult.IntegerResult(parsed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
