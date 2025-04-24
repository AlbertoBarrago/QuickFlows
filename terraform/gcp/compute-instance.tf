# Google Cloud Platform Compute Instance Terraform Example

# Configure the Google Cloud Provider
provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

# Variables
variable "project_id" {
  description = "Google Cloud Project ID"
  type        = string
}

variable "region" {
  description = "GCP region to deploy resources"
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "GCP zone to deploy resources"
  type        = string
  default     = "us-central1-a"
}

variable "machine_type" {
  description = "Machine type for the compute instance"
  type        = string
  default     = "e2-medium"
}

# VPC Network
resource "google_compute_network" "vpc_network" {
  name                    = "terraform-network"
  auto_create_subnetworks = "true"
}

# Firewall rule to allow HTTP, SSH, and ICMP
resource "google_compute_firewall" "allow_http_ssh" {
  name    = "allow-http-ssh"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["22", "80"]
  }

  allow {
    protocol = "icmp"
  }

  source_ranges = ["0.0.0.0/0"]
}

# Compute Instance
resource "google_compute_instance" "vm_instance" {
  name         = "terraform-instance"
  machine_type = var.machine_type
  zone         = var.zone

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-10"
    }
  }

  network_interface {
    network = google_compute_network.vpc_network.name
    access_config {
      # Ephemeral IP
    }
  }

  metadata_startup_script = <<-EOF
    #!/bin/bash
    apt-get update
    apt-get install -y apache2
    systemctl start apache2
    systemctl enable apache2
    echo "<h1>Hello from Google Cloud Terraform</h1>" > /var/www/html/index.html
    EOF

  tags = ["web", "dev"]
}

# Static IP Address
resource "google_compute_address" "static_ip" {
  name = "terraform-static-ip"
}

# Output the instance IP address
output "instance_ip" {
  value       = google_compute_instance.vm_instance.network_interface[0].access_config[0].nat_ip
  description = "The public IP address of the instance"
}

# Usage:
# 1. Initialize Terraform: terraform init
# 2. Plan the deployment: terraform plan -var="project_id=your-project-id"
# 3. Apply the configuration: terraform apply -var="project_id=your-project-id"
# 4. Destroy resources when done: terraform destroy -var="project_id=your-project-id"