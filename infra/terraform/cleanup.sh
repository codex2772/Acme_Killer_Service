#!/bin/bash
# ============================================
# Cleanup script to delete all JewelERP resources
# Run this ONCE to clean up before re-applying
# ============================================

set -e

AWS_REGION="ap-south-1"
APP_NAME="jewel-erp"

echo "🗑️  Deleting JewelERP AWS resources in $AWS_REGION..."
echo ""

# ================================
# 1. Delete ECS Service (must be first)
# ================================
echo "1/10 Deleting ECS service..."
aws ecs update-service \
  --cluster "${APP_NAME}-cluster" \
  --service "${APP_NAME}-service" \
  --desired-count 0 \
  --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  Service not found, skipping"

aws ecs delete-service \
  --cluster "${APP_NAME}-cluster" \
  --service "${APP_NAME}-service" \
  --force \
  --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  Service not found, skipping"

# ================================
# 2. Deregister Task Definitions
# ================================
echo "2/10 Deregistering task definitions..."
TASK_ARNS=$(aws ecs list-task-definitions \
  --family-prefix "$APP_NAME" \
  --query 'taskDefinitionArns[]' \
  --output text \
  --region "$AWS_REGION" 2>/dev/null) || true

for ARN in $TASK_ARNS; do
  aws ecs deregister-task-definition --task-definition "$ARN" --region "$AWS_REGION" 2>/dev/null || true
  echo "  Deregistered: $ARN"
done

# ================================
# 3. Delete ECS Cluster
# ================================
echo "3/10 Deleting ECS cluster..."
aws ecs delete-cluster \
  --cluster "${APP_NAME}-cluster" \
  --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  Cluster not found, skipping"

# ================================
# 4. Delete ALB Listener
# ================================
echo "4/10 Deleting ALB listener..."
ALB_ARN=$(aws elbv2 describe-load-balancers \
  --names "${APP_NAME}-alb" \
  --query 'LoadBalancers[0].LoadBalancerArn' \
  --output text \
  --region "$AWS_REGION" 2>/dev/null) || true

if [ -n "$ALB_ARN" ] && [ "$ALB_ARN" != "None" ]; then
  LISTENER_ARNS=$(aws elbv2 describe-listeners \
    --load-balancer-arn "$ALB_ARN" \
    --query 'Listeners[].ListenerArn' \
    --output text \
    --region "$AWS_REGION" 2>/dev/null) || true

  for LISTENER in $LISTENER_ARNS; do
    aws elbv2 delete-listener --listener-arn "$LISTENER" --region "$AWS_REGION" 2>/dev/null || true
    echo "  Deleted listener: $LISTENER"
  done
fi

# ================================
# 5. Delete ALB
# ================================
echo "5/10 Deleting ALB..."
if [ -n "$ALB_ARN" ] && [ "$ALB_ARN" != "None" ]; then
  aws elbv2 delete-load-balancer \
    --load-balancer-arn "$ALB_ARN" \
    --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  ALB not found, skipping"
  echo "  Waiting for ALB to be deleted..."
  sleep 30
fi

# ================================
# 6. Delete Target Group
# ================================
echo "6/10 Deleting target group..."
TG_ARN=$(aws elbv2 describe-target-groups \
  --names "${APP_NAME}-tg" \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text \
  --region "$AWS_REGION" 2>/dev/null) || true

if [ -n "$TG_ARN" ] && [ "$TG_ARN" != "None" ]; then
  aws elbv2 delete-target-group \
    --target-group-arn "$TG_ARN" \
    --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  Target group not found, skipping"
fi

# ================================
# 7. Delete ECR Repository
# ================================
echo "7/10 Deleting ECR repository..."
aws ecr delete-repository \
  --repository-name "$APP_NAME" \
  --force \
  --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  ECR repo not found, skipping"

# ================================
# 8. Delete Security Groups
# ================================
echo "8/10 Deleting security groups..."
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=${APP_NAME}-vpc" \
  --query 'Vpcs[0].VpcId' \
  --output text \
  --region "$AWS_REGION" 2>/dev/null) || true

if [ -n "$VPC_ID" ] && [ "$VPC_ID" != "None" ]; then
  SG_IDS=$(aws ec2 describe-security-groups \
    --filters "Name=vpc-id,Values=$VPC_ID" \
    --query "SecurityGroups[?GroupName!='default'].GroupId" \
    --output text \
    --region "$AWS_REGION" 2>/dev/null) || true

  for SG in $SG_IDS; do
    aws ec2 delete-security-group --group-id "$SG" --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  SG $SG skipped (may have dependencies)"
  done
fi

# ================================
# 9. Delete Subnets, Route Tables, IGW
# ================================
echo "9/10 Deleting VPC components..."
if [ -n "$VPC_ID" ] && [ "$VPC_ID" != "None" ]; then
  # Delete subnets
  SUBNET_IDS=$(aws ec2 describe-subnets \
    --filters "Name=vpc-id,Values=$VPC_ID" \
    --query 'Subnets[].SubnetId' \
    --output text \
    --region "$AWS_REGION" 2>/dev/null) || true

  for SUBNET in $SUBNET_IDS; do
    aws ec2 delete-subnet --subnet-id "$SUBNET" --region "$AWS_REGION" 2>/dev/null || true
    echo "  Deleted subnet: $SUBNET"
  done

  # Delete route table associations and route tables (non-main)
  RT_IDS=$(aws ec2 describe-route-tables \
    --filters "Name=vpc-id,Values=$VPC_ID" \
    --query "RouteTables[?Associations[0].Main!=\`true\`].RouteTableId" \
    --output text \
    --region "$AWS_REGION" 2>/dev/null) || true

  for RT in $RT_IDS; do
    ASSOC_IDS=$(aws ec2 describe-route-tables \
      --route-table-ids "$RT" \
      --query 'RouteTables[0].Associations[].RouteTableAssociationId' \
      --output text \
      --region "$AWS_REGION" 2>/dev/null) || true
    for ASSOC in $ASSOC_IDS; do
      aws ec2 disassociate-route-table --association-id "$ASSOC" --region "$AWS_REGION" 2>/dev/null || true
    done
    aws ec2 delete-route-table --route-table-id "$RT" --region "$AWS_REGION" 2>/dev/null || true
    echo "  Deleted route table: $RT"
  done

  # Detach and delete internet gateway
  IGW_IDS=$(aws ec2 describe-internet-gateways \
    --filters "Name=attachment.vpc-id,Values=$VPC_ID" \
    --query 'InternetGateways[].InternetGatewayId' \
    --output text \
    --region "$AWS_REGION" 2>/dev/null) || true

  for IGW in $IGW_IDS; do
    aws ec2 detach-internet-gateway --internet-gateway-id "$IGW" --vpc-id "$VPC_ID" --region "$AWS_REGION" 2>/dev/null || true
    aws ec2 delete-internet-gateway --internet-gateway-id "$IGW" --region "$AWS_REGION" 2>/dev/null || true
    echo "  Deleted IGW: $IGW"
  done
fi

# ================================
# 10. Delete VPC
# ================================
echo "10/10 Deleting VPC..."
if [ -n "$VPC_ID" ] && [ "$VPC_ID" != "None" ]; then
  aws ec2 delete-vpc --vpc-id "$VPC_ID" --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  VPC deletion failed (may have remaining dependencies)"
fi

# ================================
# Delete IAM Roles
# ================================
echo "Cleaning up IAM roles..."
for ROLE in "${APP_NAME}-ecs-task-execution-role" "${APP_NAME}-ecs-task-role"; do
  # Detach managed policies
  POLICIES=$(aws iam list-attached-role-policies --role-name "$ROLE" --query 'AttachedPolicies[].PolicyArn' --output text 2>/dev/null) || true
  for POLICY in $POLICIES; do
    aws iam detach-role-policy --role-name "$ROLE" --policy-arn "$POLICY" 2>/dev/null || true
  done
  # Delete inline policies
  INLINE=$(aws iam list-role-policies --role-name "$ROLE" --query 'PolicyNames[]' --output text 2>/dev/null) || true
  for POLICY in $INLINE; do
    aws iam delete-role-policy --role-name "$ROLE" --policy-name "$POLICY" 2>/dev/null || true
  done
  # Delete role
  aws iam delete-role --role-name "$ROLE" 2>/dev/null || echo "  ⏭️  Role $ROLE not found, skipping"
done

# ================================
# Delete CloudWatch Log Group
# ================================
echo "Deleting CloudWatch log group..."
aws logs delete-log-group \
  --log-group-name "/ecs/${APP_NAME}" \
  --region "$AWS_REGION" 2>/dev/null || echo "  ⏭️  Log group not found, skipping"

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "Next steps:"
echo "  1. Merge your PR to main"
echo "  2. Run Infrastructure pipeline → terraform plan"
echo "  3. Run Infrastructure pipeline → terraform apply"
echo "  4. Run Deploy to ECS pipeline"
