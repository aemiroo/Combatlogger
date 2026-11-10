package com.keurig.combatlogger.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChatTimeFormatTest {

    @Test
    public void formatsMillisecondsAndSeconds() {
        assertEquals("0s", Chat.timeFormat(0L));
        assertEquals("250ms", Chat.timeFormat(250L));
        assertEquals("1s", Chat.timeFormat(1000L));
    }

    @Test
    public void roundsPositiveSubsecondDurationsUpWhenRequested() {
        assertEquals("1s", Chat.timeFormat(1L, true));
        assertEquals("1s", Chat.timeFormat(500L, true));
        assertEquals("2s", Chat.timeFormat(1500L, true));
    }

    @Test
    public void formatsMinutesAndHours() {
        assertEquals("1m 0s", Chat.timeFormat(60000L));
        assertEquals("1h 0m 0s", Chat.timeFormat(3600000L));
    }
}
