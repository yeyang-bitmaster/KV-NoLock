import kvlearning.SkipListNoLock;

public class TestNoLock {
    public static void main(String[] args) {
        // 创建一个 SkipListNoLock 实例
        SkipListNoLock<Integer, Integer> SkipListNoLock = new SkipListNoLock<>();

        // 插入一些数据
        SkipListNoLock.insertNode(1, 2);
        SkipListNoLock.insertNode(4, 3);
        SkipListNoLock.insertNode(9, 4);
        SkipListNoLock.insertNode(10, 5);
        SkipListNoLock.insertNode(30, 6);
        SkipListNoLock.insertNode(40, 7);
        SkipListNoLock.insertNode(50, 8);
        SkipListNoLock.insertNode(70, 9);
        SkipListNoLock.insertNode(100, 10);
        SkipListNoLock.insertNode(60, 11);
        SkipListNoLock.insertNode(1, 12); // 更新键为1的值
        SkipListNoLock.insertNode(4, 13); // 更新键为4的值

        // 显示跳表内容
        System.out.println("原始跳表内容:");
        SkipListNoLock.displaySkipList();

        // 保存跳表到文件
        SkipListNoLock.dumpFile();
        System.out.println("跳表已保存到文件 store");

        // 清空跳表
        SkipListNoLock = new SkipListNoLock<>();
        System.out.println("跳表已清空");

        // 从文件加载跳表
        SkipListNoLock.loadFile();
        System.out.println("跳表已从文件 store 加载");

        // 显示加载后的跳表内容
        System.out.println("加载后的跳表内容:");
        SkipListNoLock.displaySkipList();

        // 验证加载后的数据是否正确
        System.out.println("验证加载后的数据:");
        System.out.println("查找键 1: " + SkipListNoLock.searchNode(1)); // 应该返回 12
        System.out.println("查找键 4: " + SkipListNoLock.searchNode(4)); // 应该返回 13
        System.out.println("查找键 9: " + SkipListNoLock.searchNode(9)); // 应该返回 4
        System.out.println("查找键 100: " + SkipListNoLock.searchNode(100)); // 应该返回 10
        System.out.println("查找键 200: " + SkipListNoLock.searchNode(200)); // 应该返回 not found

        SkipListNoLock<String, String> SkipListNoLock1 = new SkipListNoLock<>();

        // 插入一些数据
        SkipListNoLock1.insertNode("hello", "bit");
        SkipListNoLock1.insertNode("ohayo", "sjtu");
        System.out.println("原始跳表内容:");
        SkipListNoLock1.displaySkipList();

        // 保存跳表到文件
        SkipListNoLock1.dumpFile();
        System.out.println("跳表已保存到文件 store");

    }
}
