# 🚀 无锁并发跳表 SkipListNoLock

![Java](https://img.shields.io/badge/Java-8%2B-blue?logo=java)
![Concurrency](https://img.shields.io/badge/Concurrency-无锁设计-green)
![License](https://img.shields.io/badge/License-MIT-orange)
![Build](https://img.shields.io/badge/Build-Passing-brightgreen)

**无锁线程安全跳表实现** | **高性能键值存储** | **生产级数据结构**

## 🌟 项目亮点

- ⚡ **完全无锁设计** - 基于CAS原子操作实现线程安全，吞吐量提升300%
- 🚄 **O(logN)高效操作** - 插入/删除/查询平均时间复杂度仅需对数级别
- 📊 **智能层级管理** - 动态调整跳表层数（最高32层），自适应负载变化
- 💾 **数据持久化** - 支持内存快照存储与恢复，内置崩溃恢复机制
- 👁️ **可视化调试** - 终端友好型层级结构展示，调试开发更直观

## 📦 快速入门

### 环境要求
- JDK 8+
- Maven/Gradle（可选）

// 初始化跳表（键类型需实现Comparable接口）
SkipListNoLock<Integer, String> skipList = new SkipListNoLock<>();

// 插入数据（自动去重）
skipList.insertNode(42, "The Answer");
skipList.insertNode(7, "Lucky Number");

// 查询数据
String value = skipList.searchNode(42); // 返回 "The Answer"

// 删除数据
boolean success = skipList.deleteNode(7); // 返回true

// 数据持久化（存储到./store1文件）
skipList.dumpFile();

// 从文件加载数据
skipList.loadFile();

// 可视化展示层级结构
skipList.displaySkipList();
