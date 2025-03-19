package kvlearning;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Search for element in skip list
public class SkipList<K extends Comparable<?super K>,V> {

//SkipList的构造方法：
public SkipList() {
        this.header=new Node<>(null,null,MAX_LEVEL);
        this.skipListLevel=0;
        this.nodeCount=0;
}



//xtends Comparable<K> 表示 K 必须实现 Comparable<K> 接口，这意味着 K 类型的对象可以进行比较（例如用于排序或跳跃表的层级查找
private static class Node<K extends Comparable<? super K>, V>{
    K key;
    V value;
    int level;
    ArrayList<Node<K,V>> forwards;

    Node(K key,V value,int level){
        this.key = key;
        this.value = value;
        this.level = level;
        //forward.get(0) 指向该节点在第一层的下一个节点；forward.get(1) 指向该节点在第二层的下一个节点，forward.get(2) 指向该节点在第三层的下一个节点。
        this.forwards=new ArrayList<>(Collections.nCopies(level+1,null));
    }
    public K getKey(){
        return key;
    }
    public V getValue(){
        return value;
    }
    public void setValue(V value){
        this.value = value;
    }
    }
    /**
     * 跳表中的基础属性
     */
    public static final int MAX_LEVEL=32;
    private Node<K,V> header;
    private int nodeCount;
    private int  skipListLevel;
    private static final String STORE_FILE = "./store";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * 搜索跳表中是否存在键为 key 的节点
     * @param key 待查找节点的 key
     * @return 跳表中存在键为 key 的键值对返回 true，不存在返回 false
     */
    //跳表查找的是键值，不是value
    public V searchNode(K key) {
        Node<K, V> current = this.header;

        for (int i = this.skipListLevel; i >= 0; i--) {
            while (current.forwards.get(i) != null && key.compareTo(current.forwards.get(i).getKey()) > 0) {
                current = current.forwards.get(i);
            }
        }

        current = current.forwards.get(0);
        if (current != null && key.compareTo(current.getKey()) == 0) {
            return current.getValue();
        } else {
            return (V) "not found";
        }
    }

    public synchronized boolean insertNode(K key, V value) {
        Node<K, V> current = this.header;
        ArrayList<Node<K, V>> update = new ArrayList<>(Collections.nCopies(MAX_LEVEL + 1, null));

        for (int i = this.skipListLevel; i >= 0; i--) {
            while (current.forwards.get(i) != null && key.compareTo(current.forwards.get(i).getKey()) > 0) {
                current = current.forwards.get(i);
            }
            update.set(i, current);
        }
        current = current.forwards.get(0);
        if (current != null && current.getKey().compareTo(key) == 0) { // 如果 key 已经存在
            // 更新 key 对应的 value
            current.setValue(value);
            return true;
        }
        // 生成节点随机层数
        int randomLevel = randomLevel();

        if (current == null || current.getKey().compareTo(key) != 0) {

            if (randomLevel > skipListLevel) {
                for (int i = skipListLevel + 1; i < randomLevel + 1; i++) {
                    update.set(i, header);
                }
                skipListLevel = randomLevel;  // 更新跳表的当前高度
            }

            Node<K, V> insertNode = createNode(key, value, randomLevel);

            // 修改跳表中的指针指向
            for (int i = 0; i <= randomLevel; i++) {
                insertNode.forwards.set(i, update.get(i).forwards.get(i));
                update.get(i).forwards.set(i, insertNode);
            }
            nodeCount++;
            return true;
        }
        return false;
    }


    private static int randomLevel(){
        int level = 1;
        Random random=new Random();
        while(random.nextInt(2)==1)level++;
        return Math.min(level,MAX_LEVEL);
    }

    private Node<K,V> createNode(K key,V value,int level){
        Node<K,V> node = new Node<>(key,value,level);
        return node;
    }
    public synchronized boolean deleteNode(K key) {
        Node<K, V> current = this.header;
        ArrayList<Node<K, V>> update = new ArrayList<>(Collections.nCopies(MAX_LEVEL + 1, null));

        for (int i = this.skipListLevel; i >= 0; i--) {
            while (current.forwards.get(i) != null && key.compareTo(current.forwards.get(i).getKey()) > 0){
                current = current.forwards.get(i);
            }
            update.set(i, current);
        }
        current = current.forwards.get(0);

        // 搜索到 key
        if (current != null && current.getKey().compareTo(key) == 0) {
            for (int i = 0; i < this.skipListLevel; i++) {

                if (update.get(i).forwards.get(i) != current) break;

                update.get(i).forwards.set(i, current.forwards.get(i));
            }
        }

        while (this.skipListLevel > 0 && this.header.forwards.get(this.skipListLevel) == null) {
            this.skipListLevel--;
        }

        this.nodeCount--;
        return true;
    }
    /**
     * 持久化跳表内的数据
     */
    public void dumpFile() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(STORE_FILE))) {
            Node<K, V> node = this.header.forwards.get(0);
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
                Node<K, V> node = getKeyValueFromString(data);
                if (node != null) {
                    insertNode(node.getKey(), node.getValue());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file", e);
        }
    }
    private Node<K, V> getKeyValueFromString(String data) {
        if (!isValidString(data)) return null;
        String[] parts = data.split(":");
        if (parts.length < 2) return null;
        String keyStr = parts[0];
        String valueStr = parts[1].replace(";", "");
        try {
            // 假设 K 是 Integer 类型，需要解析字符串
            K key = (K) Integer.valueOf(keyStr); // 根据实际类型调整
            V value = (V) valueStr;
            return new Node<>(key, value, 1);
        } catch (Exception e) {
            return null;
        }
    }
    private boolean isValidString(String data) {
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
    List<Node<K, V>> baseLevelNodes = new ArrayList<>();
    Map<K, Integer> keyToColumn = new HashMap<>();
    Node<K, V> current = this.header.forwards.get(0);
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
    int maxLevel = this.skipListLevel;
    List<List<String>> levelRows = new ArrayList<>(maxLevel + 1);
    for (int i = 0; i <= maxLevel; i++) {
        levelRows.add(new ArrayList<>(Collections.nCopies(numColumns, "")));
    }

    // 填充各层的列数据
    for (int level = 1; level <= maxLevel; level++) {
        Node<K, V> node = this.header.forwards.get(level);
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




    //测试函数
//    public static void main(String[] args) {
//        /*
//                           +------------+
//                           |  select 60 |
//                           +------------+
//level 4     +-->1+                                                      100
//                 |
//                 |
//level 3         1+-------->10+------------------>50+           70       100
//                                                   |
//                                                   |
//level 2         1          10         30         50|           70       100
//                                                   |
//                                                   |
//level 1         1    4     10         30         50|           70       100
//                                                   |
//                                                   |
//level 0         1    4   9 10         30   40    50+-->60      70       100


}
