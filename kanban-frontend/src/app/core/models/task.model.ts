export interface Task {
  id: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: Priority;
  dueDate?: Date;
  projectId: number;
  assignedUser?: AssignedUser;
  position: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface TaskStatus {
  id: number;
  name: string;
  color: string;
  projectId: number;
  position: number;
  isDefault: boolean;
  taskCount?: number;
}

export interface TaskStatusRequest {
  name: string;
  color: string;
  position?: number;
}

export interface ReorderStatusRequest {
  statusIds: number[];
}

export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH'
}

export interface AssignedUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  avatarUrl?: string;
}

export interface TaskRequest {
  title: string;
  description?: string;
  statusId: number;
  priority?: Priority;
  dueDate?: Date;
  projectId: number;
  assignedTo?: number;
}

export interface UpdateStatusRequest {
  statusId: number;
  position?: number;
}
