package org.cleanas2.util

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.boon.criteria.Criterion
import org.boon.criteria.internal.Operator
import org.joda.time.DateTime

import java.nio.file.Files
import java.nio.file.Path

/**
 * Helper classes/functions for using boon's dataRepo.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object RepoCriteria {

    private val logger = LogFactory.getLog(RepoCriteria::class.java.simpleName)

    /**
     * Verifies that a file/path exists (path is stored as a Path object).
     */
    fun pathExists(fieldName: String): Criterion<Path> {
        return object : BooleanCriteria<Path>(fieldName) {
            override fun resolve(o: Any): Boolean {
                return Files.exists(fieldValueTyped())
            }
        }
    }

    /**
     * Verifies that a date is after "now".  "Now" is defined as when this function is called.
     */
    fun dateBeforeNow(fieldName: String): Criterion<DateTime> {
        return object : BooleanCriteria<DateTime>(fieldName) {
            private val now = DateTime.now()
            override fun resolve(o: Any): Boolean {
                return fieldValueTyped().isBefore(now)
            }
        }
    }


    /**
     * Base class allowing creation of boolean criteria (return true/false in response to a test)
     * for a Data Repo.  Just a little easier, and common, way of doing this rather than re-implementing
     * this kind of functionality in each case
     */
    abstract class BooleanCriteria<ITEM>(name: String)// we must use a NON-INDEXED operator, so that boon doesn't try to store the value (null) in the
    // dictionary.  It doesn't matter what other operator we pick.  I used 'matches' because it isn't implemented
        : Criterion<ITEM>(name, Operator.MATCHES, null as ITEM?) {

        /**
         * Returns the value from super.fieldValue(), but cast to the type you specified.
         */
        fun fieldValueTyped(): ITEM {
            return super.fieldValue() as ITEM
        }

        override fun getValue(): ITEM {
            throw UnsupportedOperationException("BooleanCriteria cannot call getValue or getValues.  This class does not have input values")
        }

        override fun getValues(): Array<ITEM> {
            throw UnsupportedOperationException("BooleanCriteria cannot call getValue or getValues.  This class does not have input values")
        }


    }

}
