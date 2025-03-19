# Java Components in Pulumi

Pulumi Components are abstractions that typically aggregate sets of related resources, in order to encapsulate implementation details.


## Characteristics
- Simple classes extending `Pulumi.ComponentResource`
- Expose Pulumi outputs as class attributes
- A class contstructor
  - Parameters:
    -  A name for the component
    -  Any optional parameters- e.g. `WebEnvironmentArgs` custom class
    -  ComponentResourceOptions- a bag of settings that controls resource behavior.  For example, resource deletion protection or explicit dependencies to other resources
- Superclass consturctor:
   	- We create a custom resource type for reference later and pass it to the superclass along with the component name and options. e.g. `my-package:index:my-custom-resource`
- Resource declarations
    - We build all of our resources within the constructor, and register our outputs
    - Resources are primarily declarative in nature
    - Thee Java builder pattern is used extensively for resource configuration
	- Resources refer to each others' output properties, forming implicit relationships, which are used to calculate dependency ordering at runtime.
	- We can also explicitly define relationships in the component options, via `parent` or `dependsOn`
- Imperative actions
  - We can perform imperative actions during execution, such fetching information from APIs or databases, reading and files, logging, etc.

## Packaging
-  The component is packaged using Gradle (and Maven is supported as well)
-  The project is both a library, for use in other Java programs, and an application with a shim that allows us to register the component with the Pulumi runtime and make it accessible to other Pulumi languages.

## Distribution
- The component source code is stored in a git repository and referenced directly from `git` - here the grade wrapper is required.
