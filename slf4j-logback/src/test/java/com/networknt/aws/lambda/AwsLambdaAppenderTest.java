/*
 * jlib - Open Source Java Library
 *
 *     www.jlib.org
 *
 *
 *     Copyright 2005-2018 Igor Akkerman
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.networknt.aws.lambda;

import org.junit.jupiter.api.Assertions;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


import org.apache.log4j.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;

public class AwsLambdaAppenderTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AwsLambdaAppenderTest.class);
    private ByteArrayOutputStream byteOut;
    private PrintStream originalStandardOut;

    @BeforeEach
    public void mockStandardOut() {
        byteOut = new ByteArrayOutputStream();

        originalStandardOut = System.out;

        System.setOut(new PrintStream(byteOut));
    }

    @AfterEach
    public void revertStandardOut() {
        System.setOut(originalStandardOut);
    }

    @Test
    public void xmlConfigNoRequestId() {

        // given
        // logback.xml present

        // when
        log.info("14m6d4 15 cöö1");

        // then
        assertThat(byteOut.toString())
                .matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\] <request-id-not-set-by-lambda-runtime> INFO  c.n.a.l.AwsLambdaAppenderTest - 14m6d4 15 cöö1" + lineSeparator() + "$");    }

    @Test
    public void xmlConfigDebugShouldNotBeLogged() {

        // given
        // logback.xml present

        // when
        log.debug("14m6d4 15 höt");

        // then
        Assertions.assertTrue(byteOut.toString().isEmpty());
    }

    @Test
    public void xmlConfigMdcRequestId() {

        String requestIdMdcKey = "AWSRequestId";

        // given
        // logback.xml present and
        MDC.put(requestIdMdcKey, "l099in95-3a5y-w1th-jlib-1t5c0015tuff");

        // when
        log.info("14m6d4 15 cöö1");

        // then
        assertThat(byteOut.toString())
                .matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\] <l099in95-3a5y-w1th-jlib-1t5c0015tuff> INFO  c.n.a.l.AwsLambdaAppenderTest - 14m6d4 15 cöö1" + lineSeparator() + "$");

        // cleanup
        MDC.remove(requestIdMdcKey);
    }

    @Test
    public void programmaticConfig() {

        // given
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();

        encoder.setPattern("[%level] <%logger> %msg%n");
        encoder.setContext(lc);
        encoder.start();

        AwsLambdaAppender appender = new AwsLambdaAppender();
        appender.setEncoder(encoder);
        appender.setContext(lc);
        appender.start();

        Logger logger = (Logger) LoggerFactory.getLogger("special.Logger");
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false); /* set to true if root should log too */

        // when
        logger.debug("14m6d4 15 höt");

        // then
        Assertions.assertTrue(byteOut.toString().contains("[DEBUG] <special.Logger> 14m6d4 15 höt" + lineSeparator()));
    }

}
