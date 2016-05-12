/*
 * Copyright Debezium Authors.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.relational;

import java.util.function.Predicate;

import io.debezium.annotation.Immutable;
import io.debezium.function.Predicates;

/**
 * Define predicates determines whether tables or columns should be used. The predicates use rules to determine which tables
 * and columns are included or excluded.
 * <p>
 * Because tables can be included and excluded based upon their fully-qualified names and based upon the database names, this
 * class defines a {@link #tableSelector() builder} to collect the various regular expression patterns the predicate(s) will use
 * to determine which columns and tables are included. The builder is then used to
 * {@link TableSelectionPredicateBuilder#build() build} the immutable table selection predicate.
 * <p>
 * By default all columns in included tables will be selected, except when they are specifically excluded using regular
 * expressions that match the columns' fully-qualified names. Therefore, the predicate is constructed using a simple
 * {@link #excludeColumns(String) static method}.
 * 
 * @author Randall Hauch
 */
@Immutable
public class Selectors {
    
    /**
     * Obtain a new {@link TableSelectionPredicateBuilder builder} for a table selection predicate.
     * 
     * @return the builder; never null
     */
    public static TableSelectionPredicateBuilder tableSelector() {
        return new TableSelectionPredicateBuilder();
    }

    /**
     * A builder of {@link Selectors}.
     * 
     * @author Randall Hauch
     */
    public static class TableSelectionPredicateBuilder {
        private Predicate<String> dbInclusions;
        private Predicate<String> dbExclusions;
        private Predicate<TableId> tableInclusions;
        private Predicate<TableId> tableExclusions;

        /**
         * Specify the names of the databases that should be included. This method will override previously included and
         * {@link #excludeDatabases(String) excluded} databases.
         * 
         * @param databaseNames the comma-separated list of database names to include; may be null or empty
         * @return this builder so that methods can be chained together; never null
         */
        public TableSelectionPredicateBuilder includeDatabases(String databaseNames) {
            if (databaseNames == null || databaseNames.trim().isEmpty()) {
                dbInclusions = null;
            } else {
                dbInclusions = Predicates.includes(databaseNames);
            }
            return this;
        }

        /**
         * Specify the names of the databases that should be excluded. This method will override previously {@link
         * #excludeDatabases(String) excluded} databases, although {@link #includeDatabases(String) including databases} overrides
         * exclusions.
         * 
         * @param databaseNames the comma-separated list of database names to exclude; may be null or empty
         * @return this builder so that methods can be chained together; never null
         */
        public TableSelectionPredicateBuilder excludeDatabases(String databaseNames) {
            if (databaseNames == null || databaseNames.trim().isEmpty()) {
                dbExclusions = null;
            } else {
                dbExclusions = Predicates.excludes(databaseNames);
            }
            return this;
        }

        /**
         * Specify the names of the tables that should be included. This method will override previously included and
         * {@link #excludeTables(String) excluded} table names.
         * <p>
         * Note that any specified tables that are in an {@link #excludeDatabases(String) excluded database} will not be included.
         * 
         * @param fullyQualifiedTableNames the comma-separated list of fully-qualified table names to include; may be null or
         *            empty
         * @return this builder so that methods can be chained together; never null
         */
        public TableSelectionPredicateBuilder includeTables(String fullyQualifiedTableNames) {
            if (fullyQualifiedTableNames == null || fullyQualifiedTableNames.trim().isEmpty()) {
                tableInclusions = null;
            } else {
                tableInclusions = Predicates.includes(fullyQualifiedTableNames, TableId::toString);
            }
            return this;
        }

        /**
         * Specify the names of the tables that should be excluded. This method will override previously {@link
         * #excludeDatabases(String) excluded} tables, although {@link #includeTables(String) including tables} overrides
         * exclusions.
         * <p>
         * Note that any specified tables that are in an {@link #excludeDatabases(String) excluded database} will not be included.
         * 
         * @param fullyQualifiedTableNames the comma-separated list of fully-qualified table names to exclude; may be null or
         *            empty
         * @return this builder so that methods can be chained together; never null
         */
        public TableSelectionPredicateBuilder excludeTables(String fullyQualifiedTableNames) {
            if (fullyQualifiedTableNames == null || fullyQualifiedTableNames.trim().isEmpty()) {
                tableExclusions = null;
            } else {
                tableExclusions = Predicates.excludes(fullyQualifiedTableNames, TableId::toString);
            }
            return this;
        }

        /**
         * Build the {@link Predicate} that determines whether a table identified by a given {@link TableId} is to be included.
         * 
         * @return the table selection predicate; never null
         * @see #includeDatabases(String)
         * @see #excludeDatabases(String)
         * @see #includeTables(String)
         * @see #excludeTables(String)
         */
        public Predicate<TableId> build() {
            Predicate<TableId> tableFilter = tableInclusions != null ? tableInclusions : tableExclusions;
            Predicate<String> dbFilter = dbInclusions != null ? dbInclusions : dbExclusions;
            if (dbFilter != null) {
                if (tableFilter != null) {
                    return (id) -> dbFilter.test(id.catalog()) && tableFilter.test(id);
                }
                return (id) -> dbFilter.test(id.catalog());
            }
            if (tableFilter != null) {
                return tableFilter;
            }
            return (id) -> true;
        }
    }

    /**
     * Build the {@link Predicate} that determines whether a column identified by a given {@link ColumnId} is to be included,
     * using the given comma-separated regular expression patterns defining which columns (if any) should be <i>excluded</i>.
     * <p>
     * Note that this predicate is completely independent of the table selection predicate, so it is expected that this predicate
     * be used only <i>after</i> the table selection predicate determined the table containing the column(s) is to be used.
     * 
     * @param fullyQualifiedTableNames the comma-separated list of fully-qualified table names to exclude; may be null or
     *            empty
     * @return this builder so that methods can be chained together; never null
     */
    public static Predicate<ColumnId> excludeColumns(String fullyQualifiedTableNames) {
        return Predicates.excludes(fullyQualifiedTableNames, ColumnId::toString);
    }
}
