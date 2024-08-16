public class TestData {

    private String testPassed;
    private float testTime;

    public TestData(String testPassed, float testTime) {
        this.testPassed = testPassed;
        this.testTime = testTime;
    }

    public String getTestStatus() {
        return testPassed;
    }

    public float getTestTime() {
        return testTime;
    }
}
