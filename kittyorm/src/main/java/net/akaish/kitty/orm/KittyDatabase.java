
/*
 * ---
 *
 *  Copyright (c) 2018 Denis Bogomolov (akaish)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is a part of KittyORM project (KittyORM library), more information at
 * https://akaish.github.io/KittyORMPages/license/
 *
 * ---
 */

package net.akaish.kitty.orm;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.util.SparseArray;

import net.akaish.kitty.orm.annotations.KITTY_DATABASE;
import net.akaish.kitty.orm.configuration.KittyConfigurator;
import net.akaish.kitty.orm.configuration.adc.KittyConfiguratorADC;
import net.akaish.kitty.orm.configuration.conf.KittyDBHelperConfiguration;
import net.akaish.kitty.orm.configuration.adc.KittyAnnoDBHelperConfigurationUtil;
import net.akaish.kitty.orm.configuration.conf.KittyDatabaseConfiguration;
import net.akaish.kitty.orm.configuration.conf.KittyTableConfiguration;
import net.akaish.kitty.orm.dumputils.scripts.KittySQLiteDumpScript;
import net.akaish.kitty.orm.exceptions.KittyExternalDBHasNotSupportedUserVersionException;
import net.akaish.kitty.orm.exceptions.KittyExternalDBSchemaMismatchException;
import net.akaish.kitty.orm.exceptions.KittyRuntimeException;
import net.akaish.kitty.orm.exceptions.KittyUnableToOpenDatabaseException;
import net.akaish.kitty.orm.query.PragmaTableInfoQuery;
import net.akaish.kitty.orm.util.KittyLog;
import net.akaish.kitty.orm.query.CreateDropHelper;
import net.akaish.kitty.orm.query.KittySQLiteQuery;
import net.akaish.kitty.orm.util.KittySchemaColumnDefinition;
import net.akaish.kitty.orm.util.KittySchemaDefinition;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;
import static net.akaish.kitty.orm.util.KittyConstants.EQUAL_SIGN;
import static net.akaish.kitty.orm.util.KittyConstants.WHITESPACE;
import static net.akaish.kitty.orm.util.KittyLog.LOG_LEVEL.I;
import static net.akaish.kitty.orm.util.KittyLog.kLog;

/**
 * Abstract class to be used with KittyKitty sqlite
 * Child must be annotated with {@link KITTY_DATABASE} annotation.
 * Also it can be annotated with {@link net.akaish.kitty.orm.annotations.KITTY_DATABASE_HELPER} annotation.
 * @author akaish (Denis Bogomolov)
 */
public abstract class  KittyDatabase<M extends KittyModel> {

    /**
     * Map of Models\Mappers classes to be used with this instance of KittyDatabase
     * May be generated automatically or set manually for faster initialization by implementing
     * {@link #getStaticModelMapperInstancesStorage()}
     */
    protected Map<Class<M>, Class<KittyMapper>> registry = null;

    /**
     * Map of Model classes as keys and ready to use pairs of instances of Models and Mappers
     * May be generated automatically or set manually for faster initialization by implementing
     * {@link #getStaticRegistry()}
     */
    protected Map<Class<M>, KittyMMEntry> modelMapperInstanceStorage = null;

    protected static final String AME_NULLED = "[KittyORM Bootstrap] Model class or\\and mapper class are\\is NULL, aborting generating MMInstanceStorage for {0} : {1} : {2}!";
    protected static final String AME_NO_KDB_ANNO_PRESENT = "[KittyORM Bootstrap] Instance of KittyDatabase has to be annotated with KITTY_DATABASE annotation but it wasn't found at {0}!";
    protected static String LI_DB_PWD_SET = "[KittyORM Bootstrap] KittyORM received password for DB encryption. Be sure this feature is implemented!";
    protected static String LI_STATIC_REGISTRY_SET = "[KittyORM Bootstrap] KittyORM received static registry!";
    protected static String LI_STATIC_MM_SET = "[KittyORM Bootstrap] KittyORM received static MM instances storage!";
    protected static String LI_DB_CONFIGURATION_ACQUIRED = "[KittyORM Bootstrap] KittyORM acquired configuration object!";
    protected static String LI_DB_HELPER_ACQUIRING = "[KittyORM Bootstrap] KittyORM acquiring database helper instance!";
    protected static String LI_DB_HELPER_ACQUIRED = "[KittyORM Bootstrap] KittyORM acquire database helper instance!";
    protected static String LI_DB_MMFACTORY_INITIATED = "[KittyORM Bootstrap] KittyORM Model Mapper factory instance created!";
    protected static String LI_SETTING_AUTOGENERATED_REGISTRY = "[KittyORM Bootstrap] KittyORM registry set from dabase configuration object";
    protected static String LI_MM_INSTANCE_STORAGE_GENERATING = "[KittyORM Bootstrap] KittyORM Model Mapper instance storage generating...";
    protected static String LI_MM_INSTANCE_STORAGE_GENERATED = "[KittyORM Bootstrap] KittyORM Model Mapper instance storage generated!";
    protected static String LI_HC_INFO = "Helper configuration acquired: schema name: {0}; schema version {1}";
    private static String IA_NULL_CONTEXT = "Context can't be NULL!";

    protected final KittyDatabaseConfiguration databaseConfiguration;
    protected final KittyDBHelperConfiguration helperConfiguration;
    protected KittyDatabaseHelper databaseHelper;

    protected final Context context;
    protected final KittyMMEntryFactory modelMapperEntryFactory;
    protected final String databasePassword;

    protected final String databaseFilePath;

    protected final boolean logOn;
    protected final String logTag;

    protected final KittySchemaDefinition expectedDefinition;

    /**
     * KittyORM main database class that represents bootstrap and holder for all related with database
     * components.
     * <br> See {@link #KittyDatabase(Context, String, String)} for more info.
     * @param ctx
     */
    protected KittyDatabase(Context ctx) {
        this(ctx, null, null, null);
    }

    /**
     * KittyORM main database class that represents bootstrap and holder for all related with database
     * components.
     * <br> See {@link #KittyDatabase(Context, String, String)} for more info.
     * @param ctx
     * @param databaseFilePath
     */
    protected KittyDatabase(Context ctx, String databaseFilePath) {
        this(ctx, null, databaseFilePath, null);
    }

    /**
     * KittyORM main database class that represents bootstrap and holder for all related with database
     * components.
     * <br> <b>What happens here?</b>
     *
     * <br> 1) Setting database password if not null. (!) Database encryption not implemented in basic
     * KittyORM {@link KittyDatabaseHelper}, so, you have to implement it yourself.
     *
     * <br> 2) Call of {@link #getStaticRegistry()}. his method is not implemented here, you have to implement it
     * yourself. It just sets registry storage to null, so it can be filled via reflection at
     * database configuration routine.
     * But it is expensive operation and after designing your database it would be good point to optimise it and create
     * registry storage manually via overriding {@link #getStaticRegistry()}.
     *
     * <br> 3) Call of {@link #getStaticModelMapperInstancesStorage()}. This method is not implemented here, you have to implement it
     * yourself. It just sets Model-Mapper storage to null, so it can be filled via reflection with
     * {@link #generateMMInstanceStorageFromRegistry(Map, KittyMMEntryFactory, KittyDatabaseConfiguration)}.
     * But it is expensive operation and after designing your database it would be good point to optimise it and create
     * model mapper instance storage manually via overriding {@link #getStaticModelMapperInstancesStorage()}.
     *
     * <br> 4) Setting {@link KittyDatabaseConfiguration} from {@link #getConfigurator(Context, Map, String, int)}. If
     * you want to use your own configurator than you should override {@link #getConfigurator(Context, Map, String, int)}
     * in your implementation to use your own implementation of {@link KittyConfigurator}.
     *
     * <br> 5) Setting helper configuration with {@link #getDBHelperConfiguration()}. You can also override it
     * for your personal purposes if necessary.
     *
     * <br> 6) Setting model mapper entry factory. This factory would be used for creating instances of model\mapper
     * via reflection for autogenerated mm instance storage.
     *
     * <br> 7) Setting registry from database configuration that was generated before. Would be called
     * only if {@link #getStaticRegistry()} returned null before.
     *
     * <br> 8) Setting model\mapper instance storage with {@link #generateMMInstanceStorageFromRegistry(Map, KittyMMEntryFactory, KittyDatabaseConfiguration)}.
     * Would be called only if {@link #getStaticModelMapperInstancesStorage()} returned null before;
     *
     * <br> 9) Setting new database helper with {@link #newDatabaseHelper()}. If you want to use your own
     * implementation of {@link KittyDatabaseHelper}, for example, for implementing database encryption,
     * override {@link #newDatabaseHelper()}.
     *
     * <br> 10) Setting predefined creation script to helper via {@link #getPreGeneratedCreateStatements(KittyDatabaseConfiguration)}.
     * If you want some performance optimisation, you can override it to use static collection of create schema statements sequence.
     *
     *
     * <br> 11) Setting predefined drop script to helper via {@link #getPreGeneratedDropStatements(KittyDatabaseConfiguration)}.
     * If you want some performance optimisation, you can override it to use static collection of drop schema statements sequence.
     *
     *
     * @param ctx Anroid context, not null.
     * @param databasePassword database password, note that you have to use your own implementation of
     *                         {@link KittyDatabaseHelper} that supports database encryption as well as
     *                         you have to override {@link #newDatabaseHelper()} to pass it into
     *                         KittyDatabase implementation.
     */
    protected KittyDatabase(Context ctx, String databasePassword, String databaseFilepath, KittySchemaDefinition expectedDefinition) {
        databaseFilePath = databaseFilepath;
        // Checking that implementation has KDB annotation and setting logging options
        KITTY_DATABASE kAnno = null;
        this.expectedDefinition = expectedDefinition;
        if(getClass().isAnnotationPresent(KITTY_DATABASE.class)) {
            kAnno = getClass().getAnnotation(KITTY_DATABASE.class);
            logOn = kAnno.isLoggingOn();
            logTag = kAnno.logTag();
        } else {
            throw new KittyRuntimeException(format(AME_NO_KDB_ANNO_PRESENT, getClass().getCanonicalName()));
        }

        // Checking application context
        if(ctx == null)
            throw new IllegalArgumentException(IA_NULL_CONTEXT);
        context = ctx;

        // Than we can set optional databaseClass password
        // I suppose that this is task of application developer to store it
        // cause on annotation configuration there is no sense in it
        this.databasePassword = databasePassword;
        if(this.databasePassword != null) {
            log(I, LI_DB_PWD_SET, true, null);
        }

        // Than we invoke to methods that do nothing here but can
        // set manually registry and mm storage in order to avoid
        // setting them with reflection
        registry = getStaticRegistry();
        if(registry!=null) {
            log(I, LI_STATIC_REGISTRY_SET, true, null);
        }
        modelMapperInstanceStorage = getStaticModelMapperInstancesStorage();
        if(modelMapperInstanceStorage!=null) {
            log(I, LI_STATIC_MM_SET, true, null);
        }

        int externalDBVersion = -1;
        if(kAnno.useExternalDatabase() && databaseFilepath != null) {
            externalDBVersion = getExternalDatabaseVersion(kAnno, databaseFilepath);
        }

        if(kAnno.useExternalDatabase() && databaseFilepath != null && this.expectedDefinition != null) {
            checkExternalDatabaseSchema(kAnno, expectedDefinition, databaseFilepath);
        }

        // Now getting databaseClass configuration
        databaseConfiguration = getConfigurator(ctx, registry, databaseFilepath, externalDBVersion).generateDatabaseConfiguration();
        log(I, LI_DB_CONFIGURATION_ACQUIRED, true, null);

        // Also getting helper configuration and modelMapperEntryFactory ready for this databaseClass
        log(I, LI_DB_HELPER_ACQUIRING, true, null);
        helperConfiguration = getDBHelperConfiguration();
        log(I, format(LI_HC_INFO, helperConfiguration.schemaName, helperConfiguration.schemaVersion), true, null);
        databaseHelper = newDatabaseHelper();

        databaseHelper.setCreateSchemaAutogeneratedScript(getPreGeneratedCreateStatements(databaseConfiguration));
        databaseHelper.setDropSchemaAutogeneratedScript(getPreGeneratedDropStatements(databaseConfiguration));

        databaseHelper.setAfterCreateScript(afterCreateScript());
        databaseHelper.setAfterMigrateScript(afterMigrateScript());
        log(I, LI_DB_HELPER_ACQUIRED, true, null);

        modelMapperEntryFactory = getModelMapperFactory(databaseConfiguration, databasePassword);
        log(I, LI_DB_MMFACTORY_INITIATED, true, null);

        // Than we check that registry and modelMapperInstanceStorage not set before and set them with
        // default methods
        if(registry==null) {
            registry = databaseConfiguration.registry;
            log(I, LI_SETTING_AUTOGENERATED_REGISTRY, true, null);
        }

        if(modelMapperInstanceStorage == null) {
            log(I, LI_MM_INSTANCE_STORAGE_GENERATING, true, null);
            modelMapperInstanceStorage = generateMMInstanceStorageFromRegistry(registry, modelMapperEntryFactory, databaseConfiguration);
            log(I, LI_MM_INSTANCE_STORAGE_GENERATED, true, null);
        }
    }

    /**
     * Returns true if provided int array contains provided int value (simple search)
     * @param array
     * @param value
     * @return
     */
    private static final boolean inArray(int[] array, int value) {
        for(int i : array) {
            if(i == value) return true;
        }
        return false;
    }

    protected final static String LI_EDB_GETTING_EXTERNAL_DB_VERSION = "[KittyORM Bootstrap] Getting database version [{0}]...";
    protected final static String LI_EDB_GETTING_EXTERNAL_DB_VERSION_SUCCESS = "[KittyORM Bootstrap] Requested database [{0}] has version {1}!";

    protected final static String LI_EDB_CHECKING_EXTERNAL_DB_SCHEMA = "[KittyORM Bootstrap] Checking external database schema [{0}]...";

    /**
     * Returns external database version
     * @param anno
     * @param databaseFilepath
     * @return external database version (PRAGMA.user_version)
     * @throws  KittyUnableToOpenDatabaseException if unable to open DB
     * @throws  KittyExternalDBHasNotSupportedUserVersionException if fetched version not listed in {@link KITTY_DATABASE#supportedExternalDatabaseVersionNumbers()}
     */
    protected int getExternalDatabaseVersion(KITTY_DATABASE anno, String databaseFilepath) {
        Log.i(anno.logTag(), format(LI_EDB_GETTING_EXTERNAL_DB_VERSION, databaseFilepath));
        SQLiteDatabase database = null;
        try {
            database = openDatabaseConnectionReadOnly(databaseFilepath);
        } catch (SQLiteException e) {
            throw new KittyUnableToOpenDatabaseException(databaseFilepath, e);
        }
        if(!database.isOpen())
            throw new KittyUnableToOpenDatabaseException(databaseFilepath);
        int version = database.getVersion();
        Log.i(anno.logTag(), format(LI_EDB_GETTING_EXTERNAL_DB_VERSION_SUCCESS, databaseFilepath, version));
        database.close();
        if(anno.supportedExternalDatabaseVersionNumbers().length > 0) {
            if(!inArray(anno.supportedExternalDatabaseVersionNumbers(), version)) {
                throw new KittyExternalDBHasNotSupportedUserVersionException(databaseFilepath,
                        version, anno.supportedExternalDatabaseVersionNumbers());
            } else {
                return version;
            }
        } else {
            return version;
        }
    }

    private static final int PRAGMA_TABLE_INFO_COLUMN_CID = 0;
    private static final int PRAGMA_TABLE_INFO_COLUMN_NAME = 1;
    private static final int PRAGMA_TABLE_INFO_COLUMN_TYPE = 2;
    private static final int PRAGMA_TABLE_INFO_COLUMN_NOT_NULL = 3;
    private static final int PRAGMA_TABLE_INFO_COLUMN_PK = 5;
    private static final int EXCEPTION_ON_GETTING_SCHEMA = 6;
    private static final int TABLE_NOT_FOUND = 7;
    private static final int EXPECTED_COLUMN_NOT_FOUND = 8;

    /**
     * Return true if all ok with expected schema, else - throws exception
     * @param anno
     * @param definition
     * @param databaseFilePath
     * @return
     */
    protected boolean checkExternalDatabaseSchema(KITTY_DATABASE anno, KittySchemaDefinition definition, String databaseFilePath) {
        Log.i(anno.logTag(), format(LI_EDB_GETTING_EXTERNAL_DB_VERSION, databaseFilePath));
        SQLiteDatabase database = null;
        try {
            database = openDatabaseConnectionReadOnly(databaseFilePath);
        } catch (SQLiteException e) {
            throw new KittyUnableToOpenDatabaseException(databaseFilePath, e);
        }
        if(!database.isOpen())
            throw new KittyUnableToOpenDatabaseException(databaseFilePath);
        Iterator<String> expectedTableNameIterator = definition.getExpectedTableNames().iterator();
        while (expectedTableNameIterator.hasNext()) {
            String expectedTableName = expectedTableNameIterator.next();
            SparseArray<KittySchemaColumnDefinition> expectedTableColumns = definition.getTableDefinition(expectedTableName);
            KittySQLiteQuery query = new PragmaTableInfoQuery(expectedTableName).getSQLQuery();
            Cursor cursor = database.rawQuery(query.toString(), null);
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                        String errMessage = null;
                        Exception any = null;
                        int errCode = -1;
                        try {
                            int cid = cursor.getInt(PRAGMA_TABLE_INFO_COLUMN_CID);
                            String name = cursor.getString(PRAGMA_TABLE_INFO_COLUMN_NAME);
                            String type = cursor.getString(PRAGMA_TABLE_INFO_COLUMN_TYPE);
                            int notNull = cursor.getInt(PRAGMA_TABLE_INFO_COLUMN_NOT_NULL);
                            int pk = cursor.getInt(PRAGMA_TABLE_INFO_COLUMN_PK);
                            if(expectedTableColumns.get(cid) != null) {
                                if(name.equals(expectedTableColumns.get(cid).getName())) {
                                    if(type.equalsIgnoreCase(expectedTableColumns.get(cid).getType().name())) {
                                        if(notNull == expectedTableColumns.get(cid).getNotNull()) {
                                            if(pk == expectedTableColumns.get(cid).getPk()) {
                                                expectedTableColumns.get(cid).setChecked(true);
                                            } else {
                                                errCode = PRAGMA_TABLE_INFO_COLUMN_PK;
                                                StringBuilder sb = new StringBuilder();
                                                sb.append("For column with cid ").append(expectedTableName).append(".").append(cid)
                                                        .append(" found column PK flag '").append(pk)
                                                        .append("' but expected '")
                                                        .append(expectedTableColumns.get(cid).getPk())
                                                        .append("';");
                                                errMessage = sb.toString();
                                            }
                                        } else {
                                            errCode = PRAGMA_TABLE_INFO_COLUMN_NOT_NULL;
                                            StringBuilder sb = new StringBuilder();
                                            sb.append("For column with cid ").append(expectedTableName).append(".").append(cid)
                                                    .append(" found column NotNull flag '").append(notNull)
                                                    .append("' but expected '")
                                                    .append(expectedTableColumns.get(cid).getNotNull())
                                                    .append("';");
                                            errMessage = sb.toString();
                                        }
                                    } else {
                                        errCode = PRAGMA_TABLE_INFO_COLUMN_TYPE;
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("For column with cid ").append(expectedTableName).append(".").append(cid)
                                                .append(" found column type '").append(type)
                                                .append("' but expected '")
                                                .append(expectedTableColumns.get(cid).getType().name())
                                                .append("';");
                                        errMessage = sb.toString();
                                    }
                                } else {
                                    errCode = PRAGMA_TABLE_INFO_COLUMN_NAME;
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("For column with cid ").append(expectedTableName).append(".").append(cid)
                                            .append(" found column name '").append(name)
                                            .append("' but expected '")
                                            .append(expectedTableColumns.get(cid).getName())
                                            .append("';");
                                    errMessage = sb.toString();
                                }
                            } else {
                                errCode = PRAGMA_TABLE_INFO_COLUMN_CID;
                                errMessage = "Found column not listed in expected columns!";
                            }
                        } catch (Exception ame) {
                            Log.e(databaseHelper.helperConfiguration.logTag, "Exception caught", ame);
                            errMessage = "Exception caught while trying to get external database schema";
                            any = ame;
                            errCode = EXCEPTION_ON_GETTING_SCHEMA;
                        }
                        if(errMessage != null) {
                            KittyRuntimeException rethrow = new KittyExternalDBSchemaMismatchException(
                                    databaseFilePath, errMessage, errCode
                            );
                            if(any != null) rethrow.setNestedException(any);
                            throw rethrow;
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            } else {
                String errMessage = "Unable to get table_info for " + expectedTableName;
                throw new KittyExternalDBSchemaMismatchException(databaseFilePath, errMessage, TABLE_NOT_FOUND);
            }
            for(int i = 0; i < expectedTableColumns.size(); i++) {
                if(!expectedTableColumns.valueAt(i).isChecked()) {
                    String errMessage = "Column "+expectedTableName+"."+expectedTableColumns.valueAt(i).getName()+" not found at "+databaseFilePath;
                    throw new KittyExternalDBSchemaMismatchException(databaseFilePath, errMessage, EXPECTED_COLUMN_NOT_FOUND);
                }
            }
        }
        return true;
    }

    public final SQLiteDatabase openDatabaseConnectionReadOnly(String nameOrFilepath) {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(nameOrFilepath, null, SQLiteDatabase.OPEN_READONLY);
        return database;
    }

    /**
     * Returns KittyConfigurator to be used with this database
     * By default would be created {@link KittyConfiguratorADC}, however
     * you can implement any configuration storage you want.
     * <br> This method used at KittyDatabase initialization
     * @param ctx application context
     * @param registry application registry
     * @param databaseFilepath filepath to database
     * @return KittyConfigurator
     */
    protected KittyConfigurator getConfigurator(Context ctx, Map<Class<M>, Class<KittyMapper>> registry, String databaseFilepath, int databaseVersion) {
        return new KittyConfiguratorADC(ctx, registry, getClass(), databaseFilepath, databaseVersion);
    }

    protected KittyDatabaseHelper newDatabaseHelper() {
        return new KittyDatabaseHelper(context, helperConfiguration, databaseConfiguration);
    }

    /**
     * Returns static map where keys are model's classes and entries are pairs of models and mappers
     * instances. Not implemented here, should be implemented
     * by developer in order to avoid generating this map with reflection.
     * <br> Having static map filled up with normally initiated mappers would speed up KittyORM quite
     * a bit.
     * @return
     */
    protected Map<Class<M>, KittyMMEntry> getStaticModelMapperInstancesStorage() {
        return null;
    }

    /**
     * Method used for getting static registry of mappers. Not implemented here, should be implemented
     * by developer in order to avoid generating registry with reflection.
     * <br> Having static registry filled up with normally initiated mappers would speed up KittyORM quite
     * a bit.
     * @return static registry of mappers, where keys are model classes.
     */
    protected Map<Class<M>, Class<KittyMapper>> getStaticRegistry() {
        return null;
    }

    protected KittyDBHelperConfiguration getDBHelperConfiguration() {
        return KittyAnnoDBHelperConfigurationUtil
                .getDBHelperConfiguration(databaseConfiguration, getClass(), context);
    }

    protected KittyMMEntryFactory getModelMapperFactory(KittyDatabaseConfiguration configuration, String dbPwd) {
        return new KittyMMEntryFactory()
                .setDatabaseConfiguration(configuration)
                .setDatabasePassword(dbPwd);
    }

    public final <M extends KittyModel> M getModel(Class<M> recordClass) {
        if(modelMapperInstanceStorage == null) return null;
        return modelMapperInstanceStorage.get(recordClass).getModelClass().clone(recordClass);
    }

    public final <D extends KittyMapper, M extends KittyModel> D getMapper(Class<M> recordClass) {
        if(modelMapperInstanceStorage == null) return null;
        KittyMMEntry<M, D> mmEntry = modelMapperInstanceStorage.get(recordClass);
        D mapper = (D) mmEntry.getMapper().clone();
        mapper.setDatabaseHelper(databaseHelper);
        mapper.getWritableDatabase();
        return mapper;
    }

    protected <M extends KittyModel> Map<Class<M>, KittyMMEntry> generateMMInstanceStorageFromRegistry(
            Map<Class<M>, Class<KittyMapper>> registry,
            KittyMMEntryFactory entryFactory, KittyDatabaseConfiguration databaseConfiguration
    ) {
        Map<Class<M>, KittyMMEntry> mmIstanceStorage = new HashMap<>();
        Iterator<KittyTableConfiguration> ktcItearator = databaseConfiguration.tableConfigurations.iterator();
        while (ktcItearator.hasNext()) {
            KittyTableConfiguration tableConfiguration = ktcItearator.next();
            Class<KittyMapper> dataMapperClass = registry.get(tableConfiguration.modelClass);
            Class<M> modelClass = (Class<M>) tableConfiguration.modelClass;
            if(dataMapperClass != null && modelClass != null) {
                mmIstanceStorage.put(modelClass, entryFactory.newMMEntry(dataMapperClass, tableConfiguration));
            } else {
                throw new KittyRuntimeException(MessageFormat.format(AME_NULLED,
                        getClass().getCanonicalName(), modelClass.getCanonicalName(),
                        dataMapperClass.getCanonicalName()));
            }
        }
        return mmIstanceStorage;
    }

    /**
     * By implementing this via static collection of queries you would speed up KittyORM initialization.
     *
     * <br> Also
     * <br>
     *     <b> Be sure that create statements that would be returned by this method would not have schema name
     *     before table name cause for some reason on some (maybe most or all) Androids you for some reason
     *     would receive unknown database SQLite error at schema generation. E.g. use CREATE TABLE table_name
     *     instead of CREATE TABLE database_name.table_name here.</b>
     * @param dbConf
     * @return
     */
    protected LinkedList<KittySQLiteQuery> getPreGeneratedCreateStatements(KittyDatabaseConfiguration dbConf) {
        LinkedList<KittySQLiteQuery> createSchemaSequence = new LinkedList<>();
        Iterator<KittyTableConfiguration> tablesConfIterator = dbConf.tableConfigurations.iterator();
        while(tablesConfIterator.hasNext()) {
            KittyTableConfiguration tableConf = tablesConfIterator.next();
            createSchemaSequence.add(CreateDropHelper.generateCreateTableStatement(true, tableConf, true));
            List<KittySQLiteQuery> indexes = CreateDropHelper.generateCreateIndexStatements(tableConf, true);
            if(indexes!=null)
                if(indexes.size() > 0) // java.util.NoSuchElementException if empty
                    createSchemaSequence.addAll(indexes);
        }
        return createSchemaSequence;
    }

    /**
     * Override this method if you want to run custom script SQLite sequence after schema creation
     * <br> This method has higher priority than script that may be returned from any stored script
     * @return
     */
    public KittySQLiteDumpScript afterCreateScript() {
        return null;
    }

    /**
     * Override this method of you want to run custom script sequence after schema migration
     * <br> This method has higher priority than script that may be returned from any stored script
     * @return
     */
    public KittySQLiteDumpScript afterMigrateScript() {
        return null;
    }

    public void printPregeneratedCreateSchemaToLog(String logTag) {
        LinkedList<KittySQLiteQuery> pregeneratedCreateSchema = getPreGeneratedCreateStatements(databaseConfiguration);
        Iterator<KittySQLiteQuery> iterator = pregeneratedCreateSchema.iterator();
        while (iterator.hasNext()) {
            Log.d(logTag, iterator.next().getSql());
        }
    }

    public void printPregeneratedDropSchemaToLog(String logTag) {
        LinkedList<KittySQLiteQuery> pregeneratedDropSchema = getPreGeneratedDropStatements(databaseConfiguration);
        Iterator<KittySQLiteQuery> iterator = pregeneratedDropSchema.iterator();
        while (iterator.hasNext()) {
            Log.d(logTag, iterator.next().getSql());
        }
    }

    /**
     * By implementing this via static collection of queries you would speed up KittyORM initialization.
     *
     * <br> Also
     * <br>
     * <b> Be sure to implement it with such drop statements order to avoid FK constraints, cause there
     * is no way to avoid it without overcomplicated queries. </b>
     * <br>
     *    <b> And be sure that all tables in drop sequence starts with table name, i.e. Drop table SOMEDB.sometable;</b>
     *
     * @param dbConf
     * @return
     */
    protected LinkedList<KittySQLiteQuery> getPreGeneratedDropStatements(KittyDatabaseConfiguration dbConf) {
        LinkedList<KittySQLiteQuery> dropSchemaSequence = new LinkedList<>();
        Iterator<KittyTableConfiguration> tablesConfIterator = dbConf.tableConfigurations.iterator();
        while(tablesConfIterator.hasNext()) {
            KittyTableConfiguration tableConf = tablesConfIterator.next();
            dropSchemaSequence.add(CreateDropHelper.generateDropTableStatement(tableConf));
        }
        return dropSchemaSequence;
    }

    // Other stuff

    protected static String PR_MESSAGE = "Printing registry! ";

    /**
     * Prints registry to serial if logOn = true
     * @param logLevel
     */
    public final void printRegistryToLog(KittyLog.LOG_LEVEL logLevel) {
        log(logLevel, PR_MESSAGE, true, null);
        for(Map.Entry<Class<M>, Class<KittyMapper>> registryEntry : registry.entrySet()) {
            StringBuffer sb = new StringBuffer(registryEntry.getKey().getCanonicalName());
            sb.append(WHITESPACE);
            sb.append(EQUAL_SIGN);
            sb.append(registryEntry.getValue().getCanonicalName());
            log(logLevel, sb.toString(), true, null);
        }
    }

    /**
     * Deletes current database, make sure that all connections would be closed
     * @return
     */
    public final boolean deleteDatabase() {
        return deleteDatabase(databaseConfiguration.schemaName);
    }

    /**
     * Deletes requested database, make sure that all connections to this database would be closed
     * @param schemaName
     * @return
     */
    public final boolean deleteDatabase(String schemaName) {
        return context.deleteDatabase(schemaName);
    }


    protected static String SN_NOT_ACQUIRE_YET = "[schema name not acquired yet]";
    protected static String SV_NOT_ACQUIRE_YET = "[schema version not acquired yet]";
    protected static String DBH_NOT_ACQUIRE_YET = "[DB helper not created yet]";

    protected static String DBB_INFO = " [Database class: {0}; schema name: {1}; schema version: {2}; helper class: {3}]";

    /**
     * Simple method for logging in this db class if log is on. Returns message parameter if no formatting
     * should be used or formatted string where:
     * <br> {0} is value for {@link #getClass()} canonical name
     * <br> {1} is value for {@link KittyDBHelperConfiguration#schemaName}
     * <br> {2} is value for {@link KittyDBHelperConfiguration#schemaVersion}
     * <br> {4} is value for {@link KittyDatabaseHelper} used with this bootstrap class.
     * <br>
     *     TODO: I know that is the same method with just different formatter option as used in
     *     {@link KittyDatabaseHelper}, so I would rewrite it without CP (copy-paste, lol) in future.
     * @param level
     * @param message
     * @param useDefaultFormatter
     * @param tr any throwable, may be null
     * @return
     */
    protected String log(KittyLog.LOG_LEVEL level, String message,
                         boolean useDefaultFormatter, Throwable tr) {

        String schemaVersion; String schemaName; String dbHelper;

        if(databaseConfiguration == null) {
            schemaVersion = SV_NOT_ACQUIRE_YET;
            schemaName = SN_NOT_ACQUIRE_YET;
        } else {
            schemaName = databaseConfiguration.schemaName;
            schemaVersion = Integer.toString(databaseConfiguration.schemaVersion);
        }
        if(databaseHelper == null) {
            dbHelper = DBH_NOT_ACQUIRE_YET;
        } else {
            dbHelper = databaseHelper.getClass().getCanonicalName();
        }

        if(useDefaultFormatter) {
            StringBuffer sb = new StringBuffer(message);
            sb.append(format(
                    DBB_INFO,
                    getClass().getCanonicalName(),
                    schemaName,
                    schemaVersion,
                    dbHelper)
            );
        }
        if(logOn) {
            kLog(level, logTag, message, tr);
        }
        return message;
    }
}
