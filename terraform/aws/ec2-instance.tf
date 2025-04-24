# AWS EC2 Instance Terraform Example

# Configure the AWS Provider
provider "aws" {
  region = var.aws_region
}

# Variables
variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-west-2"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}

variable "ami_id" {
  description = "AMI ID for the EC2 instance"
  type        = string
  # Amazon Linux 2 AMI (HVM), SSD Volume Type
  default     = "ami-0c55b159cbfafe1f0"
}

# Security Group for EC2 instance
resource "aws_security_group" "instance_sg" {
  name        = "instance-security-group"
  description = "Allow SSH and HTTP traffic"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH access"
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP access"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name = "instance-security-group"
  }
}

# EC2 Instance
resource "aws_instance" "web_server" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  vpc_security_group_ids = [aws_security_group.instance_sg.id]
  
  user_data = <<-EOF
              #!/bin/bash
              yum update -y
              yum install -y httpd
              systemctl start httpd
              systemctl enable httpd
              echo "<h1>Hello from Terraform</h1>" > /var/www/html/index.html
              EOF
  
  tags = {
    Name = "web-server"
  }
}

# Output the public IP address
output "public_ip" {
  value       = aws_instance.web_server.public_ip
  description = "Public IP address of the web server"
}

# Usage:
# 1. Initialize Terraform: terraform init
# 2. Plan the deployment: terraform plan
# 3. Apply the configuration: terraform apply
# 4. Destroy resources when done: terraform destroy