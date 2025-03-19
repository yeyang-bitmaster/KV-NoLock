import kvlearning.SkipList;
public class Test {
    public static void main(String[] args) {
        // 创建一个 SkipList 实例
        SkipList<Integer, Integer> skipList = new SkipList<>();

        // 插入一些数据
        skipList.insertNode(1, 2);
        skipList.insertNode(4, 3);
        skipList.insertNode(9, 4);
        skipList.insertNode(10, 5);
        skipList.insertNode(30, 6);
        skipList.insertNode(40, 7);
        skipList.insertNode(50, 8);
        skipList.insertNode(70, 9);
        skipList.insertNode(100, 10);
        skipList.insertNode(60, 11);
        skipList.insertNode(1, 12); // 更新键为1的值
        skipList.insertNode(4, 13); // 更新键为4的值

        // 显示跳表内容
        System.out.println("原始跳表内容:");
        skipList.displaySkipList();

        // 保存跳表到文件
        skipList.dumpFile();
        System.out.println("跳表已保存到文件 store");

        // 清空跳表
        skipList = new SkipList<>();
        System.out.println("跳表已清空");

        // 从文件加载跳表
        skipList.loadFile();
        System.out.println("跳表已从文件 store 加载");

        // 显示加载后的跳表内容
        System.out.println("加载后的跳表内容:");
        skipList.displaySkipList();

        // 验证加载后的数据是否正确
        System.out.println("验证加载后的数据:");
        System.out.println("查找键 1: " + skipList.searchNode(1)); // 应该返回 12
        System.out.println("查找键 4: " + skipList.searchNode(4)); // 应该返回 13
        System.out.println("查找键 9: " + skipList.searchNode(9)); // 应该返回 4
        System.out.println("查找键 100: " + skipList.searchNode(100)); // 应该返回 10
        System.out.println("查找键 200: " + skipList.searchNode(200)); // 应该返回 not found

        SkipList<String, String> skipList1 = new SkipList<>();

        // 插入一些数据
        skipList1.insertNode("hello", "bit");
        skipList1.insertNode("ohayo", "sjtu");
        System.out.println("原始跳表内容:");
        skipList1.displaySkipList();

        // 保存跳表到文件
        skipList1.dumpFile();
        System.out.println("跳表已保存到文件 store");

    }
}
