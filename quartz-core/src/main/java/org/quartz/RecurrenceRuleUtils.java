package org.quartz;

import biweekly.ICalVersion;
import biweekly.io.ParseContext;
import biweekly.io.TimezoneInfo;
import biweekly.io.WriteContext;
import biweekly.io.scribe.property.RecurrenceRuleScribe;
import biweekly.parameter.ICalParameters;
import biweekly.property.RecurrenceRule;
import biweekly.util.Recurrence;

public class RecurrenceRuleUtils {

    private RecurrenceRuleUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static Recurrence parseRecurrenceRuleExpression (String recurrenceRuleExpression) {
        RecurrenceRuleScribe scribe = new RecurrenceRuleScribe();
        ParseContext context = new ParseContext();
        context.setVersion(ICalVersion.V2_0);
        RecurrenceRule rrule = scribe.parseText(recurrenceRuleExpression, null, new ICalParameters(), context);
        return rrule.getValue();
    }

    public static String generateRecurrenceRuleExpression (Recurrence rrule) {
        WriteContext context = new WriteContext(ICalVersion.V2_0, new TimezoneInfo(), null);
        RecurrenceRuleScribe scribe = new RecurrenceRuleScribe();
        return scribe.writeText(new RecurrenceRule(rrule), context);
    }

}
