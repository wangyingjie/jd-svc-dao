package com.jd.svc.dao.shard;

import com.alibaba.cobarclient.Shard;
import com.alibaba.cobarclient.route.Router;
import com.alibaba.cobarclient.transaction.MultipleCauseException;
import com.alibaba.mtc.threadpool.MtContextExecutors;
import com.jd.cobarclient.mybatis.spring.MySqlSessionTemplate;
import com.jd.cobarclient.mybatis.spring.SqlSessionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.TransactionFactory;
import org.mybatis.spring.MyBatisExceptionTranslator;
import org.mybatis.spring.SqlSessionHolder;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author: wangyingjie1
 * @version: 1.0
 * @createdate: 2017-11-02 12:37
 */
public class ShardSqlSessionTemplate extends SqlSessionTemplate implements InitializingBean {

    private Log log;
    protected Map<String, Environment> environmentMap;
    protected Set<Shard> shards;
    protected Router router;
    private TransactionFactory transactionFactory;
    private final SqlSessionFactory sqlSessionFactory;
    private final ExecutorType executorType;
    private final SqlSession sqlSessionProxy;
    private final PersistenceExceptionTranslator exceptionTranslator;
    private boolean useDefaultExecutor;
    private ExecutorService executor;

    public ShardSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType());
    }

    public ShardSqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
        this(sqlSessionFactory, executorType, new MyBatisExceptionTranslator(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(), true));
    }

    public ShardSqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType, PersistenceExceptionTranslator exceptionTranslator) {

        super(sqlSessionFactory, executorType, exceptionTranslator);

        this.log = LogFactory.getLog(MySqlSessionTemplate.class);
        this.environmentMap = new HashMap();
        this.transactionFactory = new SpringManagedTransactionFactory();
        this.useDefaultExecutor = false;
        this.executor = Executors.newFixedThreadPool(20);
        Assert.notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
        Assert.notNull(executorType, "Property 'executorType' is required");
        this.sqlSessionFactory = sqlSessionFactory;
        this.executorType = executorType;
        this.exceptionTranslator = exceptionTranslator;
        this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(SqlSessionFactory.class.getClassLoader(), new Class[]{SqlSession.class}, new ShardSqlSessionTemplate.SqlSessionInterceptor());
    }

    @Override
    public SqlSessionFactory getSqlSessionFactory() {
        return this.sqlSessionFactory;
    }

    @Override
    public ExecutorType getExecutorType() {
        return this.executorType;
    }

    @Override
    public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
        return this.exceptionTranslator;
    }

    @Override
    public <T> T selectOne(String statement) {
        return this.sqlSessionProxy.selectOne(statement);
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        return this.sqlSessionProxy.selectOne(statement, parameter);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
        return this.sqlSessionProxy.selectMap(statement, mapKey);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
        return this.sqlSessionProxy.selectMap(statement, parameter, mapKey);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
        return this.sqlSessionProxy.selectMap(statement, parameter, mapKey, rowBounds);
    }

    @Override
    public <E> List<E> selectList(String statement) {
        return this.sqlSessionProxy.selectList(statement);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        return this.sqlSessionProxy.selectList(statement, parameter);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        return this.sqlSessionProxy.selectList(statement, parameter, rowBounds);
    }

    @Override
    public void select(String statement, ResultHandler handler) {
        this.sqlSessionProxy.select(statement, handler);
    }

    @Override
    public void select(String statement, Object parameter, ResultHandler handler) {
        this.sqlSessionProxy.select(statement, parameter, handler);
    }

    @Override
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        this.sqlSessionProxy.select(statement, parameter, rowBounds, handler);
    }

    @Override
    public int insert(String statement) {
        return this.sqlSessionProxy.insert(statement);
    }

    @Override
    public int insert(String statement, Object parameter) {
        if (parameter instanceof Collection) {
            Collection collection = (Collection) parameter;
            if (CollectionUtils.isEmpty(collection)) {
                return 0;
            } else {
                return collection.size() <= 100 ? this.batchSync(statement, collection) : this.batchAsync(statement, collection);
            }
        } else {
            return this.sqlSessionProxy.insert(statement, parameter);
        }
    }

    private <T> int batchSync(String statement, Collection<T> collection) {
        Map<Shard, List<T>> classifiedEntities = this.classify(statement, collection);
        MultipleCauseException throwables = new MultipleCauseException();
        int counter = 0;
        Iterator iterator = classifiedEntities.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Shard, List<T>> entry = (Map.Entry) iterator.next();
            Environment environment = (Environment) this.environmentMap.get(((Shard) entry.getKey()).getId());
            SqlSession sqlSession = SqlSessionUtils.getSqlSession(this.sqlSessionFactory, ExecutorType.BATCH, this.exceptionTranslator, environment);

            try {
                Object unwrapped;
                try {
//                    Iterator it = ((List) entry.getValue()).iterator();
//
//                    while (it.hasNext()) {
//                        unwrapped = it.next();
//                        sqlSession.update(statement, entry.getValue());
//                    }

                    /// 批量插入插入
                    sqlSession.update(statement, entry.getValue());

                    List<BatchResult> results = sqlSession.flushStatements();
                    int[] updateCounts = ((BatchResult) results.get(0)).getUpdateCounts();

                    for (int i = 0; i < updateCounts.length; ++i) {
                        int value = updateCounts[i];
                        counter += value;
                    }
                } catch (Throwable var17) {
                    unwrapped = ExceptionUtil.unwrapThrowable(var17);
                    if (this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
                        Throwable translated = this.exceptionTranslator.translateExceptionIfPossible((PersistenceException) unwrapped);
                        if (translated != null) {
                            unwrapped = translated;
                        }
                    }

                    throwables.add((Throwable) unwrapped);
                }
            } finally {
                SqlSessionUtils.closeSqlSession(sqlSession, this.sqlSessionFactory);
            }
        }

        if (!throwables.getCauses().isEmpty()) {
            throw new TransientDataAccessResourceException("one or more errors when performing data access operations  against multiple shards", throwables);
        } else {
            return counter;
        }
    }

    private final <T> int batchAsync(String statement, Collection<T> collection) {
        Map<Shard, List<T>> classifiedEntities = this.classify(statement, collection);
        CountDownLatch latch = new CountDownLatch(classifiedEntities.size());
        List<Future<Integer>> futures = new ArrayList();
        MultipleCauseException throwables = new MultipleCauseException();
        ExecutorService _executor = MtContextExecutors.getMtcExecutorService(this.executor);
        SqlSessionHolder holder = SqlSessionUtils.currentSqlSessionHolder(this.sqlSessionFactory);
        Iterator iterator = classifiedEntities.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Shard, List<T>> entry = (Map.Entry) iterator.next();
            futures.add(_executor.submit(new ShardSqlSessionTemplate.BatchAsyncCallable(entry, statement, latch, throwables, holder)));
        }

        try {
            latch.await();
        } catch (InterruptedException var11) {
            throw new ConcurrencyFailureException("interrupted when processing data access request in concurrency", var11);
        }

        if (!throwables.getCauses().isEmpty()) {
            throw new TransientDataAccessResourceException("one or more errors when performing data access operations against multiple shards", throwables);
        } else {
            return this.counter(this.getFutureResults(futures));
        }
    }

    private int counter(List<Integer> result) {
        int counter = 0;

        Integer row;
        for (Iterator i$ = result.iterator(); i$.hasNext(); counter += row.intValue()) {
            row = (Integer) i$.next();
        }

        return counter;
    }

    private <T> List<T> getFutureResults(List<Future<T>> futures) {
        List<T> result = new ArrayList();
        Iterator<Future<T>> iterator = futures.iterator();

        while (iterator.hasNext()) {
            Future<T> future = iterator.next();
            try {
                result.add(future.get());
            } catch (InterruptedException var6) {
                throw new ConcurrencyFailureException("interrupted when processing data access request in concurrency", var6);
            } catch (ExecutionException var7) {
                throw new ConcurrencyFailureException("something goes wrong in processing", var7);
            }
        }

        return result;
    }

    @Override
    public int update(String statement) {
        return this.sqlSessionProxy.update(statement);
    }

    @Override
    public int update(String statement, Object parameter) {
        if (parameter instanceof Collection) {
            Collection collection = (Collection) parameter;
            return CollectionUtils.isEmpty(collection) ? 0 : this.batchSync(statement, collection);
        } else {
            return this.sqlSessionProxy.update(statement, parameter);
        }
    }

    @Override
    public int delete(String statement) {
        return this.sqlSessionProxy.delete(statement);
    }

    @Override
    public int delete(String statement, Object parameter) {
        return this.sqlSessionProxy.delete(statement, parameter);
    }

    @Override
    public <T> T getMapper(Class<T> type) {
        return this.getConfiguration().getMapper(type, this);
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession");
    }

    @Override
    public void commit(boolean force) {
        throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession");
    }

    @Override
    public void rollback() {
        throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession");
    }

    @Override
    public void rollback(boolean force) {
        throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Manual close is not allowed over a Spring managed SqlSession");
    }

    @Override
    public void clearCache() {
        this.sqlSessionProxy.clearCache();
    }

    @Override
    public Configuration getConfiguration() {
        return this.sqlSessionFactory.getConfiguration();
    }

    @Override
    public Connection getConnection() {
        return this.sqlSessionProxy.getConnection();
    }


    @Override
    public List<BatchResult> flushStatements() {
        return this.sqlSessionProxy.flushStatements();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.shards != null && !this.shards.isEmpty()) {
            if (this.router == null) {
                throw new IllegalArgumentException("'router' argument is required");
            } else {
                if (this.executor == null) {
                    this.useDefaultExecutor = true;
                    this.executor = Executors.newCachedThreadPool(new ThreadFactory() {

                        @Override
                        public Thread newThread(Runnable runnable) {
                            return new Thread(runnable, "Sql-Executor-thread");
                        }
                    });
                }

                Iterator i$ = this.shards.iterator();

                while (i$.hasNext()) {
                    Shard shard = (Shard) i$.next();
                    Environment environment = new Environment(shard.getId(), this.transactionFactory, shard.getDataSource());
                    this.environmentMap.put(shard.getId(), environment);
                }

            }
        } else {
            throw new IllegalArgumentException("'shards' argument is required.");
        }
    }

    private <T> Map<Shard, List<T>> classify(String statementName, Collection<T> entities) {
        Map<Shard, List<T>> shardEntityMap = new HashMap();
        Iterator iterator = entities.iterator();

        while (iterator.hasNext()) {
            Object entity = iterator.next();
            Set<Shard> shards = this.router.route(statementName, entity);

            List<T> shardEntities;
            for (Iterator it = shards.iterator(); it.hasNext(); ((List) shardEntities).add(entity)) {
                Shard shard = (Shard) it.next();
                shardEntities = (List) shardEntityMap.get(shard);
                if (null == shardEntities) {
                    shardEntities = new ArrayList();
                    shardEntityMap.put(shard, shardEntities);
                }
            }
        }

        if (shardEntityMap.size() == 0) {
            throw new RuntimeException("没有找到对应的路由信息");
        } else {
            return shardEntityMap;
        }
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public void setShards(Set<Shard> shards) {
        this.shards = shards;
    }

    public void setTransactionFactory(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    @Override
    public void destroy() throws Exception {
        if (this.useDefaultExecutor) {
            this.executor.shutdown();
        }

    }

    private class SqlSessionInterceptor implements InvocationHandler {
        private SqlSessionInterceptor() {
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Set shards;
            if (args.length == 1) {
                shards = ShardSqlSessionTemplate.this.router.route(args[0].toString(), (Object) null);
            } else {
                shards = ShardSqlSessionTemplate.this.router.route(args[0].toString(), args[1]);
            }

            SqlSession sqlSession;
            if (shards.isEmpty()) {
                ShardSqlSessionTemplate.this.log.warn(args[0] + "没有找到路由规则,选用默认数据源");
                sqlSession = SqlSessionUtils.getSqlSession(ShardSqlSessionTemplate.this.sqlSessionFactory, ShardSqlSessionTemplate.this.executorType, ShardSqlSessionTemplate.this.exceptionTranslator);
            } else {
                if (shards.size() != 1) {
                    throw new RuntimeException("暂时不支持多读或者多写操作...");
                }

                Environment environment = (Environment) ShardSqlSessionTemplate.this.environmentMap.get(((Shard) shards.iterator().next()).getId());
                sqlSession = SqlSessionUtils.getSqlSession(ShardSqlSessionTemplate.this.sqlSessionFactory, ShardSqlSessionTemplate.this.executorType, ShardSqlSessionTemplate.this.exceptionTranslator, environment);
            }

            Object unwrapped;
            try {
                Object result = method.invoke(sqlSession, args);
                if (!SqlSessionUtils.isSqlSessionTransactional(sqlSession, ShardSqlSessionTemplate.this.sqlSessionFactory)) {
                    sqlSession.commit(true);
                }

                unwrapped = result;
            } catch (Throwable var12) {
                unwrapped = ExceptionUtil.unwrapThrowable(var12);
                if (ShardSqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
                    Throwable translated = ShardSqlSessionTemplate.this.exceptionTranslator.translateExceptionIfPossible((PersistenceException) unwrapped);
                    if (translated != null) {
                        unwrapped = translated;
                    }
                }

                throw (Throwable) unwrapped;
            } finally {
                SqlSessionUtils.closeSqlSession(sqlSession, ShardSqlSessionTemplate.this.sqlSessionFactory);
            }

            return unwrapped;
        }
    }

    class BatchAsyncCallable<T> implements Callable<Integer> {
        private Map.Entry<Shard, List<T>> entry;
        private String statement;
        private CountDownLatch latch;
        private MultipleCauseException throwables;
        private SqlSessionHolder sqlSessionHolder;

        BatchAsyncCallable(Map.Entry<Shard, List<T>> entry, String statement, CountDownLatch latch, MultipleCauseException throwables, SqlSessionHolder sqlSessionHolder) {
            this.entry = entry;
            this.statement = statement;
            this.latch = latch;
            this.throwables = throwables;
            this.sqlSessionHolder = sqlSessionHolder;
        }

        @Override
        public Integer call() throws Exception {
            Environment environment = (Environment) ShardSqlSessionTemplate.this.environmentMap.get(((Shard) this.entry.getKey()).getId());
            SqlSession sqlSession = SqlSessionUtils.getSqlSession(ShardSqlSessionTemplate.this.sqlSessionFactory, ExecutorType.BATCH, ShardSqlSessionTemplate.this.exceptionTranslator, environment, this.sqlSessionHolder);
            int counter = 0;

            try {
                Object unwrapped;
                try {
//                    Iterator i$ = ((List) this.entry.getValue()).iterator();
//
//                    while (i$.hasNext()) {
//                        unwrapped = i$.next();
//                        sqlSession.update(this.statement, unwrapped);
//                    }

                    /// 批量插入
                    sqlSession.update(this.statement, this.entry.getValue());

                    List<BatchResult> results = sqlSession.flushStatements();
                    if (results.size() != 1) {
                        throw new InvalidDataAccessResourceUsageException("Batch execution returned invalid results. Expected 1 but number of BatchResult objects returned was " + results.size());
                    }

                    int[] updateCounts = ((BatchResult) results.get(0)).getUpdateCounts();

                    for (int i = 0; i < updateCounts.length; ++i) {
                        int value = updateCounts[i];
                        counter += value;
                    }

                    Integer var16 = counter;
                    return var16;
                } catch (Throwable var11) {
                    unwrapped = ExceptionUtil.unwrapThrowable(var11);
                    if (ShardSqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
                        Throwable translated = ShardSqlSessionTemplate.this.exceptionTranslator.translateExceptionIfPossible((PersistenceException) unwrapped);
                        if (translated != null) {
                            unwrapped = translated;
                        }
                    }
                }

                this.throwables.add((Throwable) unwrapped);
            } finally {
                SqlSessionUtils.closeSqlSession(sqlSession, ShardSqlSessionTemplate.this.sqlSessionFactory);
                this.latch.countDown();
            }

            return counter;
        }
    }
}