package org.ff4j.property.repository;

/*-
 * #%L
 * ff4j-core
 * %%
 * Copyright (C) 2013 - 2019 FF4J
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.ff4j.core.jdbc.JdbcUtils.buildStatement;
import static org.ff4j.core.jdbc.JdbcUtils.executeUpdate;
import static org.ff4j.core.jdbc.JdbcUtils.isTableExist;
import static org.ff4j.core.test.AssertUtils.assertHasLength;
import static org.ff4j.core.test.AssertUtils.assertNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.ff4j.core.jdbc.JdbcQueryBuilder;
import org.ff4j.core.jdbc.JdbcSchema;
import org.ff4j.property.Property;
import org.ff4j.property.exception.PropertyAccessException;
import org.ff4j.property.mapper.PropertyMapperJdbc;

/**
 * Access information related to properties within database.
 *
 * @author Cedrick Lunven (@clunven)
 */
public class PropertyRepositoryJdbc extends PropertyRepositorySupport implements JdbcSchema {

    /** serialVersionUID. */
    private static final long serialVersionUID = -1746222910983624609L;

    /** Access to storage. */
    private DataSource dataSource;
    
    /** Query builder. */
    private JdbcQueryBuilder queryBuilder;
    
    /** Default Constructor. */
    public PropertyRepositoryJdbc() {}
    
    /**
     * Constructor from DataSource.
     * 
     * @param jdbcDS
     *            native jdbc datasource
     */
    public PropertyRepositoryJdbc(DataSource jdbcDS) {
        this.dataSource = jdbcDS;
    }

    /** {@inheritDoc} */
    @Override
    public void createSchema() {
        DataSource       ds = getDataSource();
        JdbcQueryBuilder qb = getQueryBuilder();
        if (!isTableExist(ds, qb.getTableNameProperties())) {
            executeUpdate(ds, qb.sqlCreateTableProperties());
        }
    }
     
    /** {@inheritDoc} */
    @Override
    public boolean exists(String name) {
        assertHasLength(name);
        try (Connection sqlConn = getDataSource().getConnection()) {
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlExistProperty())) {
                ps1.setString(1, name);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    rs1.next();
                    return 1 == rs1.getInt(1);
                }
            }
        } catch (SQLException sqlEX) {
           throw new PropertyAccessException("Cannot check property existence, error related to database", sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void saveProperty(Property<?> ap) {
        assertNotNull(ap);
        if (exists(ap.getUid())) {
            deleteProperty(ap.getUid());
        }
        try (Connection sqlConn = getDataSource().getConnection()) {
            PropertyMapperJdbc pmapper = new PropertyMapperJdbc(sqlConn, getQueryBuilder());
            try(PreparedStatement ps1 = pmapper.mapToRepository(ap)) {
                ps1.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot update properties database, SQL ERROR", sqlEX);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public long count() {
        return findAllIds().count();
    }

    /** {@inheritDoc} */
    @Override
    public Optional < Property<?> > find(String name) {
        assertHasLength(name);
        try (Connection sqlConn = getDataSource().getConnection()) {
            PropertyMapperJdbc pmapper = new PropertyMapperJdbc(sqlConn, getQueryBuilder());
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectPropertyById())) {
                ps1.setString(1, name);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    return (rs1.next()) ? 
                            Optional.of(pmapper.mapFromRepository(rs1)) : Optional.empty();
                }
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot check property existence, error related to database", sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updatePropertyValue(String name, String newValue) {
        assertHasLength(name);
        // Check existence
        Property<?> ab = read(name);
        try (Connection sqlConn = getDataSource().getConnection()) {
            // Check new value validity
            ab.fromString(newValue);
            try(PreparedStatement ps = buildStatement(sqlConn, getQueryBuilder().updateProperty(), newValue, name)) {
                ps.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot update property database, SQL ERROR", sqlEX);
        }
    }
   
    /** {@inheritDoc} */
    @Override
    public void deleteProperty(String name) {
        assertHasLength(name);
        assertPropertyExist(name);
        try (Connection sqlConn = getDataSource().getConnection()) {
            try (PreparedStatement ps = buildStatement(sqlConn, getQueryBuilder().sqlDeleteProperty(), name)) {
                ps.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot delete property database, SQL ERROR", sqlEX);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public Stream < Property<?> > findAll() {
        Collection <Property<?>> properties = new ArrayList<>();
        try (Connection sqlConn = getDataSource().getConnection()) {
            PropertyMapperJdbc  pmapper = new PropertyMapperJdbc(sqlConn, getQueryBuilder());
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllProperties())) {
                try (ResultSet rs1 = ps1.executeQuery()) {
                    while (rs1.next()) {
                        properties.add(pmapper.mapFromRepository(rs1));
                    }
                }
            }
            return properties.stream();
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot read properties within database, SQL ERROR", sqlEX);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public Stream<String> findAllIds() {
        Collection < String > propertyNames = new ArrayList<>();
        try (Connection sqlConn = getDataSource().getConnection()) {
            try(PreparedStatement ps1 = sqlConn.prepareStatement(getQueryBuilder().sqlSelectAllPropertyNames())) {
                try (ResultSet rs1 = ps1.executeQuery()) {
                    while (rs1.next()) {
                        propertyNames.add(rs1.getString(PropertyColumns.UID.colname()));
                    }
                }
            }
            return propertyNames.stream();
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot read properties within database, SQL ERROR", sqlEX);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAll() {
        try (Connection   sqlConn = getDataSource().getConnection()) {
            try(PreparedStatement ps = buildStatement(sqlConn, getQueryBuilder().sqlDeleteAllProperties())) {
                ps.executeUpdate();
            }
        } catch (SQLException sqlEX) {
            throw new PropertyAccessException("Cannot clear properties table, SQL ERROR", sqlEX);
        }
    }

    /**
     * Getter accessor for attribute 'dataSource'.
     *
     * @return
     *       current value of 'dataSource'
     */
    public DataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource has not been initialized");
        }
        return dataSource;
    }

    /**
     * Setter accessor for attribute 'dataSource'.
     * @param dataSource
     * 		new value for 'dataSource '
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
	/**
	 * @return the queryBuilder
	 */
	public JdbcQueryBuilder getQueryBuilder() {
		if (queryBuilder == null) {
			queryBuilder = new JdbcQueryBuilder();
		}
		return queryBuilder;
	}
	
}
