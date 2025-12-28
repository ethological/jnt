# config.yml

## Structure Overview

```yaml
exempt:
  - package: <package_name>
    classes:
      - class: <class_name>
        methods:
          - <method_name>      # Exclude specific methods by name
        fields:
          - <field_name>       # Exclude specific fields by name
```

### Fields

- **package**: Specifies the package to exclude. Wildcards (`*`) are supported for global exclusions.
- **classes**: Specifies the classes within the package to exclude.
    - **class**: The name of the class to exclude. Wildcards (`*`) are supported for global exclusions.
    - **methods**: A list of methods to exclude within the class.
        - **method_name**: The name of the method to exclude.
        - **method_descriptor**: You can specify a method descriptor (e.g., `longLo32(L)`).
    - **fields**: A list of fields to exclude within the class.
        - **field_name**: The name of the field to exclude.

## Examples

### Exclude Methods in Specific Classes

```yaml
exempt:
  - package: dev.sim0n.app.test.impl.crypttest
    classes:
      - class: Blowfish
        methods:
          - longLo32      # Exclude any method with this name
      - class: Blowfish2
        methods:
          - longLo32(L)   # Exclude only the method with this descriptor
```

### Exclude Methods in a Specific Class

```yaml
exempt:
  - package: dev.sim0n.app.utils
    classes:
      - class: UtilityClass
        methods:
          - helperMethod  # Exclude this specific method in UtilityClass
```

### Exclude All Classes, Methods, and Fields Under a Package

```yaml
exempt:
  - package: dev.sim0n.app.shared.*
    classes:
      - class: *          # Exclude all classes under dev.sim0n.app.shared.*
        methods:
          - *             # Exclude all methods in all classes under dev.sim0n.app.shared.*
        fields:
          - *             # Exclude all fields in all classes under dev.sim0n.app.shared.*
```

### Global Exclusions

```yaml
excluded:
  - package: *
    classes:
      - class: *          # Exclude all classes globally
        methods:
          - *             # Exclude all methods globally
        fields:
          - serialVersionUID  # Exclude serialVersionUID field globally
```

## Notes

- Use `*` for global exclusions. For example, `package: *` will exclude all packages globally.
- Specific exclusions can be made for methods by name or descriptor.
- Wildcards can also be used for classes and methods to exclude all or match multiple items.