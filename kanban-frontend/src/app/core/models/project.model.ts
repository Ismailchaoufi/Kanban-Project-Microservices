export interface Project {
  id: number;
  title: string;
  description?: string;
  status: ProjectStatus;
  color?: string;
  startDate?: Date;
  endDate?: Date;
  ownerId: number;
  members: Member[];
  createdAt: Date;
  updatedAt: Date;
}

export enum ProjectStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED'
}

export interface Member {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  avatarUrl?: string;
  joinedAt: Date;
}

export interface ProjectRequest {
  title: string;
  description?: string;
  status?: ProjectStatus;
  color?: string;
  startDate?: Date;
  endDate?: Date;
}

export interface ProjectStats {
  projectId: number;
  projectTitle: string;
  totalTasks: number;
  todoTasks: number;
  inProgressTasks: number;
  doneTasks: number;
  totalMembers: number;
}
