/**
 * H2GIS is a library that brings spatial support to the H2 Database Engine
 * <a href="http://www.h2database.com">http://www.h2database.com</a>. H2GIS is developed by CNRS
 * <a href="http://www.cnrs.fr/">http://www.cnrs.fr/</a>.
 *
 * This code is part of the H2GIS project. H2GIS is free software; 
 * you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * H2GIS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details <http://www.gnu.org/licenses/>.
 *
 *
 * For more information, please consult: <a href="http://www.h2gis.org/">http://www.h2gis.org/</a>
 * or contact directly: info_at_h2gis.org
 */

package org.h2gis.functions.io;

import org.h2.util.StringUtils;
import org.h2gis.api.*;
import org.h2gis.functions.io.dbf.DBFDriverFunction;
import org.h2gis.functions.io.dbf.DBFEngine;
import org.h2gis.functions.io.shp.SHPDriverFunction;
import org.h2gis.functions.io.shp.SHPEngine;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

/**
 * Manage additional table engines in H2.
 * Use the appropriate driver to open a specified file path.
 * @author Nicolas Fortin
 * @author Sylvain PALOMINOS (UBS 2019)
 */
public class DriverManager extends AbstractFunction implements ScalarFunction, DriverFunction {

    private static final DriverDef[] DRIVERS = new DriverDef[] {
            new DriverDef(DBFEngine.class.getName(),"dbf"),
            new DriverDef(SHPEngine.class.getName(),"shp")};
    private static final int FORMAT = 0;
    private static final int DESCRIPTION = 1;
    private static final String[][] formatDescription = new String[][] {{"dbf", DBFDriverFunction.DESCRIPTION},
                                                                        {"shp", SHPDriverFunction.DESCRIPTION}};

    public DriverManager() {
        addProperty(PROP_NAME, "FILE_TABLE");
        addProperty(PROP_REMARKS, "Use the appropriate driver to open a specified file path.");
    }

    @Override
    public String getJavaStaticMethod() {
        return "openFile";
    }

    @Override
    public String getFormatDescription(String format) {
        for(String[] descr : formatDescription) {
            if(descr[FORMAT].equalsIgnoreCase(format)) {
                return descr[DESCRIPTION];
            }
        }
        return "";
    }

    @Override
    public boolean isSpatialFormat(String extension) {
        return extension.equalsIgnoreCase("shp");
    }

    /**
     * Create a new table
     * @param connection Active connection, do not close this connection.
     * @param fileName File path to write, if exists it may be replaced
     * @param tableName [[catalog.]schema.]table reference
     * @return The name of table formatted according the database rules
     */
    public static String[] openFile(Connection connection, String fileName, String tableName) throws SQLException {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        final DBTypes dbType = DBUtils.getDBType(connection);
        for(DriverDef driverDef : DRIVERS) {
            if(driverDef.getFileExt().equalsIgnoreCase(ext)) {
                try (Statement st = connection.createStatement()) {
                    String tableName_ = TableLocation.parse(tableName, dbType).toString();
                    st.execute(String.format("CREATE TABLE %s COMMENT %s ENGINE %s WITH %s",
                            tableName_,StringUtils.quoteStringSQL(fileName),
                            StringUtils.quoteJavaString(driverDef.getClassName()),StringUtils.quoteJavaString(fileName)));
                     return new String[]{tableName_};
                }
            }
        }
        throw new SQLException("No driver is available to open the "+ext+" file format");
    }

    /**
     * Driver declaration
     */
    private static class DriverDef {
        private String className;
        private String fileExt;

        private DriverDef(String className, String fileExt) {
            this.className = className;
            this.fileExt = fileExt;
        }

        /**
         * @return Class package and name
         */
        public String getClassName() {
            return className;
        }

        /**
         * @return File extension, case insensitive
         */
        public String getFileExt() {
            return fileExt;
        }
    }

    @Override
    public String[] exportTable(Connection connection, String tableReference, File fileName, ProgressVisitor progress)
            throws SQLException, IOException {
        throw new SQLFeatureNotSupportedException("Work in progress..");
    }

    @Override
    public String[] exportTable(Connection connection, String tableReference, File fileName, boolean deleteFiles, ProgressVisitor progress) throws SQLException, IOException {

        throw new SQLFeatureNotSupportedException("Work in progress..");
    }

    @Override
    public String[] exportTable(Connection connection, String tableReference, File fileName, String options, boolean deleteFiles, ProgressVisitor progress) throws SQLException, IOException {

        throw new SQLFeatureNotSupportedException("Work in progress..");
    }

    @Override
    public String[] exportTable(Connection connection, String tableReference, File fileName, String options, ProgressVisitor progress
                            ) throws SQLException, IOException {
        throw new SQLFeatureNotSupportedException("Work in progress..");
    }

    @Override
    public IMPORT_DRIVER_TYPE getImportDriverType() {
        return IMPORT_DRIVER_TYPE.LINK;
    }

    @Override
    public String[] getImportFormats() {
        String[] formats = new String[DRIVERS.length];
        for(int idDriver = 0; idDriver < DRIVERS.length; idDriver++) {
            formats[idDriver] = DRIVERS[idDriver].getFileExt();
        }
        return formats;
    }

    @Override
    public String[] getExportFormats() {
        return new String[0];
    }

    @Override
    public String[]  importFile(Connection connection, String tableReference, File fileName, ProgressVisitor progress)
            throws SQLException, IOException {
        return openFile(connection, fileName.getAbsolutePath(), tableReference);
    }

    @Override
    public String[]  importFile(Connection connection, String tableReference, File fileName,  String options, ProgressVisitor progress
                          ) throws SQLException, IOException {
        return openFile(connection, fileName.getAbsolutePath(), tableReference);
    }

    @Override
    public String[]  importFile(Connection connection, String tableReference, File fileName,boolean deleteTables, ProgressVisitor progress
                           ) throws SQLException, IOException {
        return openFile(connection, fileName.getAbsolutePath(), tableReference);
    }

    @Override
    public String[]  importFile(Connection connection, String tableReference, File fileName, String options, boolean deleteTables, ProgressVisitor progress) throws SQLException, IOException {
        return openFile(connection, fileName.getAbsolutePath(), tableReference);
    }



    /**
     * Method to check the import and export arguments
     * @param connection
     * @param tableReference
     * @param fileName
     * @param progress
     * @return
     * @throws SQLException
     */
    public static ProgressVisitor check(Connection connection, String tableReference, File fileName, ProgressVisitor progress) throws SQLException {
        if (connection == null) {
            throw new SQLException("The connection cannot be null.\n");
        }
        if (tableReference == null || tableReference.isEmpty()) {
            throw new SQLException("The table cannot be null or empty");
        }
        if (fileName == null) {
            throw new SQLException("The file name cannot be null.\n");
        }
        if (progress == null) {
            progress = new EmptyProgressVisitor();
        }
        return progress;
    }
}
