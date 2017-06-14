package org.quartz;

import org.junit.Assert;
import org.junit.Test;

import biweekly.util.Frequency;
import biweekly.util.Recurrence;

public class RecurrenceRuleUtilsTest {

    @Test
    public void testParseRecurrenceRuleExpression () {

        Recurrence recurrence = RecurrenceRuleUtils.parseRecurrenceRuleExpression("FREQ=MINUTELY;INTERVAL=2;BYHOUR=9,10,11,12,13,14,15,16");
        Assert.assertNotNull(recurrence);
        Assert.assertEquals(0, Frequency.MINUTELY.compareTo(recurrence.getFrequency()));
        Assert.assertTrue(recurrence.getByHour().size() == 8);

    }

    @Test
    public void testGenerateRecurrenceRuleExpression () {
        String recurrenceExpresionOriginal = "FREQ=MINUTELY;INTERVAL=2;BYHOUR=9,10,11,12,13,14,15,16";
        Recurrence recurrence = RecurrenceRuleUtils.parseRecurrenceRuleExpression(recurrenceExpresionOriginal);
        String recurrenceExpressionGenerated = RecurrenceRuleUtils.generateRecurrenceRuleExpression(recurrence);
        Assert.assertEquals(recurrenceExpresionOriginal, recurrenceExpressionGenerated);
    }

}
