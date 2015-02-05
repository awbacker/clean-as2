package org.cleanas2.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.boon.criteria.Criterion;
import org.boon.criteria.internal.Operator;
import org.joda.time.DateTime;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper classes/functions for using boon's dataRepo.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class RepoCriteria {

    private static final Log logger = LogFactory.getLog(RepoCriteria.class.getSimpleName());

    /**
     * Verifies that a file/path exists (path is stored as a Path object).
     */
    public static Criterion<Path> pathExists(final String fieldName) {
        return new BooleanCriteria<Path>(fieldName){
            @Override
            public boolean resolve(Object o) {
                return Files.exists(fieldValueTyped());
            }
        };
    }

    /**
     * Verifies that a date is after "now".  "Now" is defined as when this function is called.
     */
    public static Criterion<DateTime> dateBeforeNow(final String fieldName) {
        return new BooleanCriteria<DateTime>(fieldName) {
            private final DateTime now = DateTime.now();
            public boolean resolve(Object o) {
                return fieldValueTyped().isBefore(now);
            }
        };
    }


    /**
     * Base class allowing creation of boolean criteria (return true/false in response to a test)
     * for a Data Repo.  Just a little easier, and common, way of doing this rather than re-implementing
     * this kind of functionality in each case
     */
    public static abstract class BooleanCriteria<ITEM> extends Criterion<ITEM> {

        @SuppressWarnings("unchecked")
        public BooleanCriteria(String name) {
            // we must use a NON-INDEXED operator, so that boon doesn't try to store the value (null) in the
            // dictionary.  It doesn't matter what other operator we pick.  I used 'matches' because it isn't implemented
            super(name, Operator.MATCHES, (ITEM)null);
        }

        @SuppressWarnings("unchecked")
        /**
         * Returns the value from super.fieldValue(), but cast to the type you specified.
         */
        public ITEM fieldValueTyped() {
            return (ITEM) super.fieldValue();
        }

        @Override
        public ITEM getValue() {
            throw new UnsupportedOperationException("BooleanCriteria cannot call getValue or getValues.  This class does not have input values");
        }

        @Override
        public ITEM[] getValues() {
            throw new UnsupportedOperationException("BooleanCriteria cannot call getValue or getValues.  This class does not have input values");
        }


    }

}
