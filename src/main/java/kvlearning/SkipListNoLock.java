package kvlearning;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class SkipListNoLock<K extends Comparable<? super K>, V> {
    private static final int MAX_LEVEL = 32;
    private final Node<K, V> header;
    private final AtomicInteger skipListLevel = new AtomicInteger(0);
    private final AtomicInteger nodeCount = new AtomicInteger(0);
    private static final String STORE_FILE = "./store1";
    // 无锁节点定义
    private static class Node<K extends Comparable<? super K>, V> {
        final K key;
        final V value;
        final AtomicReferenceArray<Node<K, V>> forwards;
        final int level;

        Node(K key, V value, int level) {
            this.key = key;
            this.value = value;
            this.level = level;
            this.forwards = new AtomicReferenceArray<>(level + 1);
        }

        K getKey() { return key; }
        V getValue() { return value; }
    }

    public SkipListNoLock() {
        this.header = new Node<>(null, null, MAX_LEVEL);
    }
    public V searchNode(K key) {
        Node<K, V> current = this.header;

        for (int i = skipListLevel.get(); i >= 0; i--) {
            Node<K, V> next = current.forwards.get(i);
            while (next != null && key.compareTo(next.getKey()) > 0) {
                current = next;
                next = current.forwards.get(i);
            }
        }

        current = current.forwards.get(0);
        if (current != null && key.compareTo(current.getKey()) == 0) {
            return current.getValue();
        } else {
            return null; // 或自定义 "not found"
        }
    }

    public boolean insertNode(K key, V value) {
        Node<K, V>[] update = (Node<K, V>[]) new Node[MAX_LEVEL + 1];
        Node<K, V> current = this.header;

        // 1. 查找插入位置并填充 update 数组（覆盖 MAX_LEVEL 层）
        for (int i = MAX_LEVEL; i >= 0; i--) {  // 关键修改：遍历到 MAX_LEVEL 而非当前层数
            Node<K, V> next = current.forwards.get(i);
            while (next != null && key.compareTo(next.getKey()) > 0) {
                current = next;
                next = current.forwards.get(i);
            }
            update[i] = current;
        }

        // 2. 检查是否已存在
        Node<K, V> candidate = current.forwards.get(0);
        if (candidate != null && key.compareTo(candidate.getKey()) == 0) {
            return false;
        }

        // 3. 生成随机层数
        int randomLevel = randomLevel();

        // 4. 动态扩展跳表层数（CAS原子操作）
        int currentMaxLevel = skipListLevel.get();
        if (randomLevel > currentMaxLevel) {
            // 使用 CAS ,先进行比较，如果相比较的两个值是相等的，那么就进行更新操作
            if (skipListLevel.compareAndSet(currentMaxLevel, randomLevel)) {
                // 更新成功后，确保高层级的 update 指向 header
                for (int i = currentMaxLevel + 1; i <= randomLevel; i++) {
                    update[i] = header;
                }
            } else {
                // 其他线程已更新层数，重新获取最新层数
                currentMaxLevel = skipListLevel.get();
                randomLevel = Math.min(randomLevel, currentMaxLevel);
            }
        }

        // 5. 创建新节点
        Node<K, V> newNode = new Node<>(key, value, randomLevel);

        // 6. CAS 更新前驱节点指针（确保 update[i] 非空）
        for (int i = 0; i <= randomLevel; i++) {
            Node<K, V> pred = update[i];
            Node<K, V> expectedNext = pred.forwards.get(i);
            newNode.forwards.set(i, expectedNext);

            // 自旋直到 CAS 成功或发现竞争
            while (!pred.forwards.compareAndSet(i, expectedNext, newNode)) {
                expectedNext = pred.forwards.get(i);
                newNode.forwards.set(i, expectedNext);
            }
        }

        nodeCount.incrementAndGet();
        return true;
    }

    public boolean deleteNode(K key) {
        Node<K, V>[] update = (Node<K, V>[]) new Node[MAX_LEVEL + 1];
        Node<K, V> current = this.header;

        // 1. 查找待删除节点路径
        for (int i = skipListLevel.get(); i >= 0; i--) {
            Node<K, V> next = current.forwards.get(i);
            while (next != null && key.compareTo(next.getKey()) > 0) {
                current = next;
                next = current.forwards.get(i);
            }
            update[i] = current;
        }

        // 2. 定位目标节点
        Node<K, V> target = current.forwards.get(0);
        if (target == null || key.compareTo(target.getKey()) != 0) {
            return false;
        }

        // 3. CAS 更新前驱节点的指针，跳过目标节点
        for (int i = 0; i <= target.level; i++) {
            Node<K, V> expected = target.forwards.get(i);
            while (!update[i].forwards.compareAndSet(i, target, expected)) {
                expected = update[i].forwards.get(i);
                if (expected != target) break; // 其他线程已修改
            }
        }

        // 4. 降低跳表层数
        while (skipListLevel.get() > 0 && header.forwards.get(skipListLevel.get()) == null) {
            skipListLevel.decrementAndGet();
        }

        nodeCount.decrementAndGet();
        return true;
    }
    private int randomLevel() {
        int level = 1;
        Random random = new Random();
        while (random.nextInt(2) == 0 && level < MAX_LEVEL) {
            level++;
        }
        return level;
    }
    /**
     * 持久化跳表内的数据
     */
    public void dumpFile() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(STORE_FILE))) {
            SkipListNoLock.Node<K, V> node = this.header.forwards.get(0);
            while (node != null) {
                String data = node.getKey() + ":" + node.getValue() + ";";
                bufferedWriter.write(data);
                bufferedWriter.newLine();
                node = node.forwards.get(0);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to dump file", e);
        }
    }
    /**
     * 从文本文件中读取数据
     */
    public void loadFile() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(STORE_FILE))) {
            String data;
            while ((data = bufferedReader.readLine()) != null) {
                System.out.println(data);
                SkipListNoLock.Node<K, V> node = getKeyValueFromString(data);
                if (node != null) {
                    insertNode(node.getKey(), node.getValue());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file", e);
        }
    }
    private static <K extends Comparable<? super K>, V> Node<K, V> getKeyValueFromString(String data) {
        if (!isValidString(data)) return null;
        String[] parts = data.split(":");
        if (parts.length < 2) return null;
        String keyStr = parts[0];
        String valueStr = parts[1].replace(";", "");
        try {
            @SuppressWarnings("unchecked")
            K key = (K) Integer.valueOf(keyStr); // 根据实际类型调整
            @SuppressWarnings("unchecked")
            V value = (V) valueStr;
            return new Node<>(key, value, 1);
        } catch (Exception e) {
            return null;
        }
    }
    private static boolean isValidString(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        if (!data.contains(":")) {
            return false;
        }
        return true;
    }

    public void displaySkipList() {
        // 获取基准层（level 0）的所有节点，确定列的顺序和每个键对应的列索引
        List<SkipListNoLock.Node<K, V>> baseLevelNodes = new ArrayList<>();
        Map<K, Integer> keyToColumn = new HashMap<>();
        SkipListNoLock.Node<K, V> current = this.header.forwards.get(0);
        while (current != null) {
            baseLevelNodes.add(current);
            keyToColumn.put(current.getKey(), baseLevelNodes.size() - 1);
            current = current.forwards.get(0);
        }

        int numColumns = baseLevelNodes.size();
        if (numColumns == 0) {
            System.out.println("跳表为空");
            return;
        }

        // 初始化各层的数据结构，存储每个层每个列的字符串
        int maxLevel = this.skipListLevel.get();
        List<List<String>> levelRows = new ArrayList<>(maxLevel + 1);
        for (int i = 0; i <= maxLevel; i++) {
            levelRows.add(new ArrayList<>(Collections.nCopies(numColumns, "")));
        }
        // 填充各层的列数据
        for (int level = 1; level <= maxLevel; level++) {
            SkipListNoLock.Node<K, V> node = this.header.forwards.get(level);
            List<String> row = levelRows.get(level);
            while (node != null) {
                K key = node.getKey();
                int column = keyToColumn.get(key);
                String str = node.getKey() + ":" + node.getValue();
                row.set(column, str);
                node = node.forwards.get(level);
            }
        }

        // 计算每列的最大宽度
        int[] columnWidths = new int[numColumns];
        for (int col = 0; col < numColumns; col++) {
            int maxWidth = 0;
            for (int level = 0; level <= maxLevel; level++) {
                String str = levelRows.get(level).get(col);
                if (str.length() > maxWidth) {
                    maxWidth = str.length();
                }
            }
            columnWidths[col] = maxWidth;
        }

        // 打印各层
        for (int level = maxLevel; level >= 1; level--) {
            List<String> row = levelRows.get(level);
            StringBuilder sb = new StringBuilder();
            sb.append("Level ").append(level).append(": ");
            for (int col = 0; col < numColumns; col++) {
                String str = row.get(col);
                int width = columnWidths[col];
                String formatted = String.format("%-" + width + "s", str.isEmpty() ? "" : str);
                sb.append(formatted);
                if (col < numColumns - 1) {
                    sb.append(" ");
                }
            }
            System.out.println(sb.toString().trim());
        }
    }
}