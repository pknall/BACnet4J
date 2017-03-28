package com.serotonin.bacnet4j.obj;

import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.service.confirmed.AddListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.RemoveListElementRequest;
import com.serotonin.bacnet4j.type.constructed.CalendarEntry;
import com.serotonin.bacnet4j.type.constructed.DateRange;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.WeekNDay;
import com.serotonin.bacnet4j.type.constructed.WeekNDay.WeekOfMonth;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Date;

public class CalendarObjectTest extends AbstractTest {
    @Override
    public void before() throws Exception {
        // no op
    }

    @Test
    public void test() throws Exception {
        final TestClock clock = new TestClock();
        clock.setTime(2115, Calendar.JANUARY, 1, 12, 0, 0);

        final CalendarEntry ce = new CalendarEntry(
                new WeekNDay(Month.UNSPECIFIED, WeekOfMonth.days22to28, DayOfWeek.WEDNESDAY)); // The Wednesday during the 4th week of each month.
        final SequenceOf<CalendarEntry> dateList = new SequenceOf<>( //
                new CalendarEntry(new Date(-1, null, -1, DayOfWeek.FRIDAY)), // Every Friday.
                new CalendarEntry(
                        new DateRange(new Date(-1, Month.NOVEMBER, -1, null), new Date(-1, Month.FEBRUARY, -1, null))), // November to February
                ce);

        final CalendarObject co = new CalendarObject(d1, 0, "cal0", dateList, clock);

        co.updatePresentValue(); // November to February
        Assert.assertEquals(new Boolean(true), co.get(PropertyIdentifier.presentValue));

        clock.setTime(2115, Calendar.MARCH, 2, 12, 0, 0);
        co.updatePresentValue();
        Assert.assertEquals(new Boolean(false), co.get(PropertyIdentifier.presentValue));

        clock.setTime(2115, Calendar.MARCH, 8, 12, 0, 0); // A Friday
        co.updatePresentValue();
        Assert.assertEquals(new Boolean(true), co.get(PropertyIdentifier.presentValue));

        clock.setTime(2115, Calendar.MAY, 27, 12, 0, 0);
        co.updatePresentValue();
        Assert.assertEquals(new Boolean(false), co.get(PropertyIdentifier.presentValue));

        clock.setTime(2115, Calendar.MAY, 22, 12, 0, 0); // The Wednesday during the 4th week of each month.
        co.updatePresentValue();
        Assert.assertEquals(new Boolean(true), co.get(PropertyIdentifier.presentValue));

        // Set the time source to a time that does not match the current date list, but
        // will match a new entry.
        clock.setTime(2115, Calendar.JUNE, 17, 12, 0, 0);
        co.updatePresentValue(); // Uses the above time source.
        Assert.assertEquals(new Boolean(false), co.get(PropertyIdentifier.presentValue));

        final CalendarEntry newEntry = new CalendarEntry(new Date(-1, Month.JUNE, -1, null));
        final AddListElementRequest addReq = new AddListElementRequest(co.getId(), PropertyIdentifier.dateList, null,
                new SequenceOf<>(newEntry));
        d2.send(rd1, addReq).get();
        Assert.assertEquals(new Boolean(true), co.get(PropertyIdentifier.presentValue));

        clock.setTime(2115, Calendar.JULY, 24, 12, 0, 0);
        co.updatePresentValue(); // Uses the above time source.
        Assert.assertEquals(new Boolean(true), co.get(PropertyIdentifier.presentValue));

        final RemoveListElementRequest remReq = new RemoveListElementRequest(co.getId(), PropertyIdentifier.dateList,
                null, new SequenceOf<>(ce));
        d2.send(rd1, remReq).get();
        Assert.assertEquals(new Boolean(false), co.get(PropertyIdentifier.presentValue));

        // Check that the compensatory time works.
        co.setTimeTolerance(1000 * 60 * 3);
        clock.setTime(2115, Calendar.AUGUST, 8, 23, 58, 0);
        co.updatePresentValue(); // Uses the above time source.
        Assert.assertEquals(new Boolean(true), co.get(PropertyIdentifier.presentValue));
    }
}
