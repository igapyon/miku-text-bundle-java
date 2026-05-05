# Upstream Test Mapping

The upstream test source or test intent for `miku-text-bundle` has not been fixed yet.

When the upstream source is selected, record mappings in this shape:

```text
upstream test / intent:
  <upstream test name or behavior>

java tests:
  <JavaTestClass.testMethod>

fixtures:
  <fixture path>

focused regression:
  mvn test -Dtest=<JavaTestClass>
```
