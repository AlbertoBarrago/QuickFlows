# Terraform Examples

## What is Terraform?

Terraform is an open-source Infrastructure as Code (IaC) tool created by HashiCorp that allows you to define and provision infrastructure using a declarative configuration language. With Terraform, you can describe your entire infrastructure as code and version it like any other software project, enabling consistent, repeatable deployments across multiple cloud providers and on-premises environments.

## Why Terraform Exists

Before Infrastructure as Code tools like Terraform, infrastructure was typically provisioned manually through web consoles or custom scripts. This approach was error-prone, difficult to reproduce, and lacked proper version control. Terraform was created to bring software engineering practices to infrastructure management, allowing teams to treat infrastructure with the same rigor as application code.

## Problems Terraform Solves

- **Infrastructure Drift**: Ensures that the actual infrastructure matches the desired state defined in code
- **Multi-Cloud Complexity**: Provides a consistent workflow across different cloud providers and services
- **Collaboration Challenges**: Enables teams to work together on infrastructure using version control systems
- **Manual Provisioning Errors**: Automates the creation and modification of resources, reducing human error
- **Documentation Gaps**: The code itself serves as documentation for your infrastructure
- **Dependency Management**: Automatically handles the order of resource creation and modification based on dependencies
- **State Management**: Tracks the current state of your infrastructure and plans changes intelligently

## How to Use Terraform

### Basic Terraform Workflow

```bash
# Initialize a Terraform working directory
terraform init

# Create an execution plan
terraform plan

# Apply the changes required to reach the desired state
terraform apply

# Destroy all resources managed by the current configuration
terraform destroy
```

### Key Terraform Concepts

- **Providers**: Plugins that interact with cloud providers, SaaS providers, or other APIs
- **Resources**: Infrastructure objects managed by Terraform (e.g., virtual machines, networks)
- **Data Sources**: Read-only information fetched from providers to use in your configuration
- **Variables**: Parameterize your configurations for reusability
- **Modules**: Reusable components that encapsulate groups of resources
- **State**: Terraform's record of what infrastructure it manages
- **Outputs**: Return values from your infrastructure that can be used by other configurations

## Contents

- `aws/`: Terraform modules and configurations for AWS resources
- `azure/`: Terraform modules and configurations for Azure resources
- `gcp/`: Terraform modules and configurations for Google Cloud Platform resources

## Usage

Each subdirectory contains detailed examples with comments explaining the purpose of each resource and how to customize it for your specific needs.

## License

This project is licensed under the MIT License - see the LICENSE file in the root directory for details.