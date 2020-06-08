package hms.dbmi.ppm;

import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.PropertySource;

class OnJWTCondition implements Condition {

    @Override
    public boolean matches(
            ConditionContext context,
            AnnotatedTypeMetadata metadata) {

        return HapiProperties.getJwtAuthEnabled();
    }
}