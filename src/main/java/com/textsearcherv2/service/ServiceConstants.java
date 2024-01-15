package com.textsearcherv2.service;

import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
public class ServiceConstants {
    public static final String LINE_DELIMITER = "\n";
    public static final Set<String> PERSON_NAMES = new HashSet<>(Arrays.asList(
            "James", "John", "Robert", "Michael", "William", "David", "Richard", "Charles", "Joseph", "Thomas", "Christopher",
            "Daniel", "Paul", "Mark", "Donald", "George", "Kenneth", "Steven", "Edward", "Brian", "Ronald", "Anthony", "Kevin",
            "Jason", "Matthew", "Gary", "Timothy", "Jose", "Larry", "Jeffrey", "Frank", "Scott", "Eric", "Stephen", "Andrew",
            "Raymond", "Gregory", "Joshua", "Jerry", "Dennis", "Walter", "Patrick", "Peter", "Harold", "Douglas", "Henry",
            "Carl", "Arthur", "Ryan", "Roger"
    ));
    public static final long THREAD_WAIT_SECONDS = 60;
}
