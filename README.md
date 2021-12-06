# dag-scheduler
基于有向无环图(DAG)的策略引擎

## 策略引擎
在策略引擎中，所有节点都有如下状态：
- CREATED `//节点已经创建`
- PREPARED `//准备开始调度`
- START `//开始调度`
- WAITING `//等待执行`
- RUNNING `//正在执行`
- SUCCESS `//执行成功`
- FAILED `//执行失败`
- INEFFECTIVE `//无效节点(父节点失败后下游单依赖子节点全部无效)`
- TIMEOUT `//执行超时`

所有节点都会被引擎调度到“PREPARED”状态，<br>
根节点首先被调度到“RUNNING”状态，其下游路径节点获得“WAITING”状态，<br>
根节点执行完毕后只有符合条件的下游节点会被调度到“RUNNING”状态进行逻辑执行；<br>
同层级不符合条件的会被转化为“FAILED”状态，<br>
对应下游所有单依赖节点都会被迫变成“INEFFECTIVE”状态（说明该路径永不可达）。<br>
获得执行的节点输出的结果会被引擎路由给该路径下游所有节点，下游节点顺势获得“RUNNING”状态，<br>
在其逻辑内部对上游传递过来的结果进行判断是否使用，然后继续输出结果给下游，<br>
直到DAG终节点结束得到最后结果。<br>

## 日志细节
执行细节参考日志: [LOG.txt](./LOG.txt)

## 已实现功能
- 全路径并行图  //全图并行，全路径都必须成功
- 单路径开关图 //全图并行，单路径成功即算成功， 策略引擎使用这个模式
- 图结构和调度与业务分离
- DAG实时快照
- DAG调度历史
- 业务策略可扩展
- 业务策略可命名可落地配置

## TODO功能
- 需要调整Dag和DagNode的状态，将图结构变为“静态”。
  将调度的“动态”与图结构的“静态”解耦并隔离调度上下文信息，
  以此来支持多线程环境下单一Dag图对象实例的重复并发调度。
  - 设计[RuntimeInitializable.java](./src/main/java/com/dvbug/dag/RuntimeInitializable.java)接口提供调度运行时初始化线程上下文。
  - 基于`ThreadLocal`封装了[ThreadableField.java](./src/main/java/com/dvbug/dag/ThreadableField.java)实现，将需要线程化的字段包装进去。
  - 已经支持线程循环并发调度  
  - `DagNode`的`trace`存在bug，待修改